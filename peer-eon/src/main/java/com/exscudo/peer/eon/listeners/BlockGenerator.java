package com.exscudo.peer.eon.listeners;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.TransactionComparator;
import com.exscudo.peer.core.events.BlockEvent;
import com.exscudo.peer.core.events.IBlockEventListener;
import com.exscudo.peer.core.events.IPeerEventListener;
import com.exscudo.peer.core.events.PeerEvent;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.core.utils.Loggers;
import com.exscudo.peer.eon.DifficultyHelper;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.tasks.GenerateBlockTask;
import com.exscudo.peer.eon.state.GeneratingBalance;
import com.exscudo.peer.eon.transactions.rules.ValidationResult;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

/**
 * Creates the next block for the chain.
 */
public class BlockGenerator implements IPeerEventListener, IBlockEventListener {

	private boolean useFastGeneration = false;

	private final AtomicBoolean isGenAllowed = new AtomicBoolean();

	private final IBacklogService backlog;

	private final IBlockchainService blockchain;

	private ISigner signer;

	public BlockGenerator(IBacklogService backlog, IBlockchainService blockchain, ISigner signer) {
		this.backlog = backlog;
		this.blockchain = blockchain;
		this.signer = signer;
	}

	public boolean isInitialized() {
		return signer != null;
	}

	public ISigner getSigner() {
		return signer;
	}

	public void setSigner(ISigner signer) {
		this.signer = signer;
	}

	public boolean isUseFastGeneration() {
		return useFastGeneration;
	}

	public void setUseFastGeneration(boolean useFastGeneration) {
		this.useFastGeneration = useFastGeneration;
	}

	public Block createNextBlock(Block previousBlock, IFork fork) {

		if (!isGenAllowed.get()) {
			return null;
		}

		isGenAllowed.set(false);

		int currentTimestamp = previousBlock.getTimestamp() + Constant.BLOCK_PERIOD;

		Map<Long, Transaction> map = new HashMap<Long, Transaction>();
		Iterator<Long> indexes = backlog.iterator();
		while (indexes.hasNext() && map.size() < EonConstant.BLOCK_TRANSACTION_LIMIT) {
			Long id = indexes.next();
			Transaction tx = backlog.get(id);
			if (tx != null && !tx.isFuture(currentTimestamp)
					&& (tx.getReference() == 0
							|| blockchain.transactionMapper().containsTransaction(tx.getReference()))) {
				map.put(id, tx);
			}
		}

		Block parallelBlock = blockchain.getBlockByHeight(previousBlock.getHeight() + 1);
		while (parallelBlock != null) {
			for (Transaction tx : parallelBlock.getTransactions()) {

				if (!tx.isFuture(currentTimestamp) && (tx.getReference() == 0
						|| blockchain.transactionMapper().containsTransaction(tx.getReference()))) {
					map.put(tx.getID(), tx);
				}
			}
			parallelBlock = blockchain.getBlockByHeight(parallelBlock.getHeight() + 1);
		}

		return createBlock(previousBlock, map.values().toArray(new Transaction[0]), fork);

	}

	private Block createBlock(Block previousBlock, Transaction[] transactions, IFork fork) {

		int timestamp = previousBlock.getTimestamp() + Constant.BLOCK_PERIOD;
		long senderID = Format.MathID.pick(getSigner().getPublicKey());
		int height = previousBlock.getHeight() + 1;

		ILedger ledger = blockchain.getState(previousBlock.getSnapshot());
		IAccount generator = ledger.getAccount(senderID);

		// validate generator
		ValidationResult r = validateGenerator(generator, height);
		if (r.hasError) {
			Loggers.warning(GenerateBlockTask.class, r.cause.getMessage());
			return null;
		}

		int version = fork.getBlockVersion(timestamp);
		if (version < 0) {
			Loggers.warning(GenerateBlockTask.class, "Invalid block version.");
			return null;
		}

		Block targetBlock = previousBlock;
		if (height - EonConstant.DIFFICULTY_DELAY > 0) {
			int targetHeight = height - EonConstant.DIFFICULTY_DELAY;
			targetBlock = blockchain.getBlockByHeight(targetHeight);
		}
		byte[] generationSignature = getSigner().sign(targetBlock.getGenerationSignature());

		ITransactionHandler handler = fork.getTransactionExecutor(timestamp);
		TransactionContext ctx = new TransactionContext(timestamp, previousBlock.getHeight() + 1);

		Arrays.sort(transactions, new TransactionComparator());
		int payloadLength = 0;
		List<Transaction> payload = new ArrayList<>(transactions.length);
		for (Transaction tx : transactions) {
			int txLength = tx.getLength();
			if (payloadLength + txLength > EonConstant.BLOCK_MAX_PAYLOAD_LENGTH)
				break;

			try {
				handler.run(tx, ledger, ctx);
				payload.add(tx);
				payloadLength += txLength;
			} catch (Exception e) {
				Loggers.info(GenerateBlockTask.class, "Excluding tr({}) from block generation payload: {}",
						Format.ID.transactionId(tx.getID()), e.getMessage());
			}
		}

		long totalFee = ctx.getTotalFee();
		if (totalFee != 0) {
			IAccount creator = ledger.getAccount(senderID);
			AccountProperties.balanceRefill(creator, totalFee);
			ledger.putAccount(creator);
		}
		byte[] snapshot = ledger.getHash();

		Block newBlock = new Block();
		newBlock.setVersion(version);
		newBlock.setHeight(height);
		newBlock.setTimestamp(timestamp);
		newBlock.setPreviousBlock(previousBlock.getID());
		newBlock.setSenderID(senderID);
		newBlock.setGenerationSignature(generationSignature);
		newBlock.setTransactions(payload);
		newBlock.setSnapshot(snapshot);
		newBlock.setSignature(getSigner().sign(newBlock.getBytes()));

		long generatingBalance = AccountProperties.getDeposit(generator).getValue();
		BigInteger diff = DifficultyHelper.calculateDifficulty(newBlock, previousBlock, generatingBalance);
		newBlock.setCumulativeDifficulty(diff);

		return newBlock;

	}

	private ValidationResult validateGenerator(IAccount generator, int height) {

		if (generator == null) {
			return ValidationResult.error("Invalid generator.");
		}

		GeneratingBalance deposit = AccountProperties.getDeposit(generator);
		if (deposit.getValue() < EonConstant.MIN_DEPOSIT_SIZE) {
			return ValidationResult.error("Too small deposit.");
		}
		if (height - deposit.getHeight() < Constant.BLOCK_IN_DAY && deposit.getHeight() != 0) {
			return ValidationResult.error("The last deposit operation was made less than a day ago.");
		}

		return ValidationResult.success;

	}

	/* IBlockEventListener members */

	@Override
	public void onLastBlockChanged(BlockEvent event) {

		if (useFastGeneration) {
			isGenAllowed.set(true);
		}
	}

	@Override
	public void onBeforeChanging(BlockEvent event) {
		isGenAllowed.set(false);
	}

	/* IPeerEventListener */

	@Override
	public void onSynchronized(PeerEvent event) {
		// The blocks is synchronized with at least one node.
		isGenAllowed.set(true);
	}

	public boolean isGenerationAllowed() {
		return isGenAllowed.get();
	}

	public void allowGenerate() {
		isGenAllowed.set(true);
	}

}
