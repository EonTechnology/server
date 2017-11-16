package com.exscudo.peer.store.sqlite;

import java.util.Arrays;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.ForkProvider;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.TransactionComparator;
import com.exscudo.peer.core.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.exceptions.LifecycleException;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.IUnitOfWork;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.DifficultyHelper;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.Sandbox;
import com.exscudo.peer.eon.transactions.utils.AccountAttributes;
import com.exscudo.peer.eon.transactions.utils.AccountDeposit;
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

	public UnitOfWork(Storage connector) {
		this.connector = connector;
		this.connection = connector.getConnection();
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

		if (currBlock.getNextBlock() != 0) {
			throw new IllegalStateException("There should be no reference to next block.");
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

		block.setNextBlock(0);
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

		ensureValid(newBlock);
		headBlock = saveBlock(headBlock, newBlock);
		ledger.analyze();
		return headBlock;
	}

	private void ensureValid(Block newBlock) throws ValidateException {

		// version
		int version = ForkProvider.getInstance().getBlockVersion(newBlock.getTimestamp());
		if (version != newBlock.getVersion()) {
			throw new ValidateException("Unsupported block version.");
		}

		// timestamp
		int timestamp = headBlock.getTimestamp() + Constant.BLOCK_PERIOD;
		if (timestamp != newBlock.getTimestamp()) {
			throw new LifecycleException();
		}

		// previous block
		if (headBlock.getID() != newBlock.getPreviousBlock()) {
			throw new ValidateException("Unexpected block. Expected - " + Format.ID.blockId(newBlock.getPreviousBlock())
					+ ", current - " + Format.ID.blockId(headBlock.getID()));
		}

		IAccount generator = ledger.getAccount(newBlock.getSenderID());
		if (generator != null) {

			// block height is not transmitted over the network
			int height = headBlock.getHeight() + 1;
			newBlock.setHeight(height);

			// check generator balance
			AccountDeposit deposit = AccountDeposit.parse(generator);
			if (deposit.getValue() < EonConstant.MIN_DEPOSIT_SIZE
					|| (height - deposit.getHeight() < Constant.BLOCK_IN_DAY && deposit.getHeight() != 0)) {
				throw new ValidateException("Too small deposit.");
			}
			byte[] publicKey = AccountAttributes.getPublicKey(generator);

			// generation signature
			Block targetBlock = headBlock;
			if (height - EonConstant.DIFFICULTY_DELAY > 0) {
				int targetHeight = height - EonConstant.DIFFICULTY_DELAY;
				// ATTENTION. in case EonConstant.DIFFICULTY_DELAY < SYNC_MILESTONE_DEPTH it is
				// necessary to revise
				targetBlock = BlockHelper.getByHeight(connection, targetHeight);
			}
			if (!CryptoProvider.getInstance().verifySignature(targetBlock.getGenerationSignature(),
					newBlock.getGenerationSignature(), publicKey)) {
				throw new IllegalSignatureException("The field Generation Signature is incorrect.");
			}

			// signature
			if (!newBlock.verifySignature(publicKey)) {
				throw new IllegalSignatureException();
			}

			// snapshot
			Sandbox sandbox = new Sandbox(ledger, headBlock.getTimestamp(), headBlock.getHeight() + 1);
			Transaction[] sortedTransactions = newBlock.getTransactions().toArray(new Transaction[0]);
			Arrays.sort(sortedTransactions, new TransactionComparator());
			for (Transaction tx : sortedTransactions) {
				sandbox.execute(tx);
			}
			byte[] snapshot = sandbox.createSnapshot(newBlock.getSenderID());
			if (newBlock.getVersion() >= 2) {
				if (!Arrays.equals(snapshot, newBlock.getSnapshot())) {
					throw new ValidateException("Illegal snapshot prefix.");
				}
			}
			newBlock.setSnapshot(snapshot);

			// adds the data that is not transmitted over the network
			newBlock.setCumulativeDifficulty(
					DifficultyHelper.calculateDifficulty(newBlock, headBlock, deposit.getValue()));
			return;
		}

		throw new ValidateException("Invalid generator. " + Format.ID.accountId(newBlock.getSenderID()));

	}

	private Block saveBlock(Block prevBlock, Block newBlock) throws ValidateException {

		if (newBlock.getPreviousBlock() == 0) {
			throw new ValidateException("Previous block is not specified.");
		} else if (prevBlock.getID() != newBlock.getPreviousBlock()) {
			throw new ValidateException("Unexpected block. Expected - " + Format.ID.blockId(newBlock.getPreviousBlock())
					+ ", current - " + Format.ID.blockId(prevBlock.getID()));
		}

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

		prevBlock.setNextBlock(id);

		blockSet.put(newBlock.getPreviousBlock(), prevBlock);
		blockSet.put(id, newBlock);

		return blockSet.get(id);
	}

}
