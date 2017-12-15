package com.exscudo.peer.store.sqlite;

import java.util.Arrays;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.TransactionComparator;
import com.exscudo.peer.core.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.exceptions.LifecycleException;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.IUnitOfWork;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.DifficultyHelper;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.transactions.rules.ValidationResult;
import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.state.GeneratingBalance;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import com.exscudo.peer.store.sqlite.merkle.CachedLedger;
import com.exscudo.peer.store.sqlite.merkle.Ledgers;
import com.exscudo.peer.store.sqlite.utils.BlockHelper;
import com.exscudo.peer.store.sqlite.utils.DatabaseHelper;
import com.exscudo.peer.store.sqlite.utils.TransactionHelper;

/**
 * Basic implementation of {@code IUnitOfWork} with direct connection to DB.
 * <p>
 * All changes in DB cached in {@code ISegment} and committed on
 * {@link IUnitOfWork#commit} call.
 *
 * @see IUnitOfWork
 * @see ISegment
 */
public class UnitOfWork implements IUnitOfWork {
	private static final String TRAN_NAME = "IUnitOfWork";
	private final Storage connector;
	private final ConnectionProxy connection;

	private Storage.LockedObject blockRoot;
	private Storage.LockedObject txRoot;

	private Storage.LockedObject backlogRoot;
	private Backlog.Savepoint backlogSavepoint;

	private Block headBlock;

	private CachedSegment<Long, Transaction> backlog = new CachedSegment<Long, Transaction>() {

		@Override
		protected Transaction doGet(Long id) {
			return connector.getBacklog().get(id);
		}

		@Override
		protected void doPut(Long id, Transaction value) {
			connector.getBacklog().put(value);
		}

		@Override
		protected void doRemove(Long id) {
			connector.getBacklog().remove(id);
		}

	};

	private CachedSegment<Long, Block> blockSet = new CachedSegment<Long, Block>() {

		@Override
		protected Block doGet(Long id) {
			return BlockHelper.get(connection, id);
		}

		@Override
		protected void doPut(Long id, Block value) {
			BlockHelper.save(connection, value);
		}

		@Override
		protected void doRemove(Long id) {
			BlockHelper.remove(connection, id);
		}

	};

	private CachedSegment<Long, Transaction> transactionSet = new CachedSegment<Long, Transaction>() {

		@Override
		protected Transaction doGet(Long id) {
			return TransactionHelper.get(connection, id);
		}

		@Override
		protected void doPut(Long id, Transaction value) {
			TransactionHelper.save(connection, value);
		}

		@Override
		protected void doRemove(Long id) {
			TransactionHelper.remove(connection, id);
		}

	};

	private CachedLedger ledger;
	private IFork fork;

	public UnitOfWork(Storage connector, IFork fork) {
		this.connector = connector;
		this.connection = connector.getConnection();
		this.fork = fork;
	}

	void begin(Block block) {

		blockRoot = connector.lockBlocks();
		txRoot = connector.lockTransactions();

		backlogRoot = connector.lockObject(connector.getBacklog());
		backlogSavepoint = connector.getBacklog().createSavepoint();

		DatabaseHelper.beginTransaction(connection, TRAN_NAME);
		popTo(block.getID());
		ledger = Ledgers.newCachedLedger(connection, headBlock.getSnapshot());
	}

	private void popTo(long blockId) {

		Block currBlock = blockSet.get(connector.getLastBlock().getID());

		if (currBlock == null) {
			throw new IllegalStateException("There should be current block exist.");
		}

		Block block = blockSet.get(blockId);
		while (currBlock.getID() != blockId) {
			if (currBlock.getHeight() <= block.getHeight()) {
				throw new IllegalStateException("Failed to remove block. Invalid height.");
			}
			currBlock = pop(currBlock);
			currBlock = blockSet.get(currBlock.getPreviousBlock());
		}

		long newBlockID = currBlock.getID();
		if (newBlockID != blockId) {

			throw new IllegalStateException("Unexpected block. Expected - " + Format.ID.blockId(blockId)
					+ ", current - " + Format.ID.blockId(newBlockID));

		}

		blockSet.put(blockId, block);

		headBlock = block;
	}

	private Block pop(Block block) {

		Block removedBlock = blockSet.get(block.getID());

		for (Transaction tx : removedBlock.getTransactions()) {

			long txID = tx.getID();
			tx.setBlock(0);
			tx.setHeight(0);

			backlog.put(txID, tx);
			transactionSet.remove(txID);
		}

		blockSet.remove(block.getID());

		return removedBlock;
	}

	@Override
	public void commit() {

		synchronized (connector) {
			Difficulty diffNew = new Difficulty(headBlock);
			Difficulty currNew = new Difficulty(connector.getLastBlock());
			if (diffNew.compareTo(currNew) <= 0) {
				throw new IllegalStateException();
			}

			blockSet.commit();
			transactionSet.commit();
			backlog.commit();
			ledger.commit();

			DatabaseHelper.commitTransaction(connection, TRAN_NAME);

			connector.setLastBlock(headBlock);

			backlogRoot.unlock();
			txRoot.unlock();
			blockRoot.unlock();
		}
	}

	@Override
	public void rollback() {

		DatabaseHelper.rollbackTransaction(connection, TRAN_NAME);

		connector.getBacklog().loadSavepoint(backlogSavepoint);

		backlogRoot.unlock();
		txRoot.unlock();
		blockRoot.unlock();
	}

	@Override
	public Block pushBlock(Block newBlock) throws ValidateException {

		// version
		int version = fork.getBlockVersion(newBlock.getTimestamp());
		if (version != newBlock.getVersion()) {
			throw new ValidateException("Unsupported block version.");
		}

		ValidationResult r = null;

		// validate generator
		r = validateGenerator(newBlock, ledger, headBlock);
		if (r.hasError) {
			throw r.cause;
		}

		// validate block
		r = validateBlock(newBlock, ledger, headBlock);
		if (r.hasError) {
			throw r.cause;
		}

		// apply
		ledger = applyBlock(newBlock, ledger, fork);

		// validate state
		r = validateState(newBlock, ledger);
		if (r.hasError) {
			throw r.cause;
		}

		ledger = saveState(ledger);
		headBlock = saveBlock(newBlock);
		return headBlock;
	}

	private ValidationResult validateGenerator(Block newBlock, ILedger ledger, Block targetBlock) {

		if (ledger == null) {
			return ValidationResult.error("Unable to get ledger for block. " + Format.ID.blockId(targetBlock.getID()));
		}

		IAccount generator = ledger.getAccount(newBlock.getSenderID());
		if (generator == null) {
			return ValidationResult.error("Invalid generator. " + Format.ID.accountId(newBlock.getSenderID()));
		}

		int height = targetBlock.getHeight() + 1;
		GeneratingBalance deposit = AccountProperties.getDeposit(generator);
		if (deposit.getValue() < EonConstant.MIN_DEPOSIT_SIZE) {
			return ValidationResult.error("Too small deposit.");
		}
		if (height - deposit.getHeight() < Constant.BLOCK_IN_DAY && deposit.getHeight() != 0) {
			return ValidationResult.error("The last deposit operation was made less than a day ago");
		}

		// adds the data that is not transmitted over the network
		newBlock.setCumulativeDifficulty(
				DifficultyHelper.calculateDifficulty(newBlock, targetBlock, deposit.getValue()));

		return ValidationResult.success;

	}

	private ValidationResult validateBlock(Block newBlock, ILedger ledger, Block targetBlock) {

		// timestamp
		int timestamp = targetBlock.getTimestamp() + Constant.BLOCK_PERIOD;
		if (timestamp != newBlock.getTimestamp()) {
			return ValidationResult.error(new LifecycleException());
		}

		// previous block
		if (newBlock.getPreviousBlock() == 0) {
			return ValidationResult.error("Previous block is not specified.");
		}

		if (targetBlock.getID() != newBlock.getPreviousBlock()) {
			return ValidationResult
					.error("Unexpected block. Expected - " + Format.ID.blockId(newBlock.getPreviousBlock())
							+ ", current - " + Format.ID.blockId(targetBlock.getID()));
		}

		if (ledger == null) {
			return ValidationResult.error("Unable to get ledger for block. " + Format.ID.blockId(targetBlock.getID()));
		}

		IAccount generator = ledger.getAccount(newBlock.getSenderID());
		if (generator == null) {
			return ValidationResult.error("Invalid generator. " + Format.ID.accountId(newBlock.getSenderID()));
		}

		byte[] publicKey = AccountProperties.getPublicKey(generator);

		// generation signature
		int height = targetBlock.getHeight() + 1;
		Block generationBlock = targetBlock;
		if (height - EonConstant.DIFFICULTY_DELAY > 0) {
			int generationHeight = height - EonConstant.DIFFICULTY_DELAY;
			// ATTENTION. in case EonConstant.DIFFICULTY_DELAY < SYNC_MILESTONE_DEPTH it is
			// necessary to revise
			generationBlock = BlockHelper.getByHeight(connection, generationHeight);
		}
		if (!CryptoProvider.getInstance().verifySignature(generationBlock.getGenerationSignature(),
				newBlock.getGenerationSignature(), publicKey)) {
			return ValidationResult
					.error(new IllegalSignatureException("The field Generation Signature is incorrect."));
		}

		// signature
		if (!newBlock.verifySignature(publicKey)) {
			return ValidationResult.error(new IllegalSignatureException());
		}

		// adds the data that is not transmitted over the network
		newBlock.setHeight(targetBlock.getHeight() + 1);

		return ValidationResult.success;
	}

	private ValidationResult validateState(Block newBlock, CachedLedger ledger) {

		byte[] snapshot = ledger.getHash();
		if (newBlock.getVersion() >= 2) {

			if (!Arrays.equals(snapshot, newBlock.getSnapshot())) {
				return ValidationResult.error("Illegal snapshot prefix.");
			}
		}
		newBlock.setSnapshot(ledger.getHash());
		return ValidationResult.success;

	}

	private CachedLedger applyBlock(Block newBlock, CachedLedger ledger, IFork fork) throws ValidateException {

		ITransactionHandler handler = fork.getTransactionExecutor(newBlock.getTimestamp());
		TransactionContext ctx = new TransactionContext(newBlock.getTimestamp(), headBlock.getHeight() + 1);
		Transaction[] sortedTransactions = newBlock.getTransactions().toArray(new Transaction[0]);
		Arrays.sort(sortedTransactions, new TransactionComparator());
		for (Transaction tx : sortedTransactions) {
			handler.run(tx, ledger, ctx);
		}
		long totalFee = ctx.getTotalFee();
		if (totalFee != 0) {
			IAccount creator = ledger.getAccount(newBlock.getSenderID());
			Balance balance = AccountProperties.getBalance(creator);
			if (balance == null) {
				balance = new Balance(0);
			}
			balance.refill(totalFee);
			AccountProperties.setBalance(creator, balance);
			ledger.putAccount(creator);
		}
		return ledger;

	}

	private CachedLedger saveState(CachedLedger ledger) {
		ledger.analyze();
		return ledger;
	}

	private Block saveBlock(Block newBlock) throws ValidateException {

		if (BlockHelper.get(connector.getConnection(), newBlock.getID()) != null) {
			throw new ValidateException("Block already exists.");
		}

		long id = newBlock.getID();
		for (Transaction tx : newBlock.getTransactions()) {

			long txID = tx.getID();

			if (transactionSet.contains(tx.getID())) {
				throw new ValidateException(Format.ID.transactionId(txID) + " already in block.");
			} else if (tx.getReference() != 0 && !transactionSet.contains(tx.getReference())) {
				throw new ValidateException(Format.ID.transactionId(txID) + ". Can not find referenced transaction.");
			}

			backlog.remove(txID);
			tx.setBlock(id);
			tx.setHeight(newBlock.getHeight());
			transactionSet.put(txID, tx);
		}

		blockSet.put(id, newBlock);

		return blockSet.get(id);
	}

}
