package com.exscudo.peer.eon.listeners;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.TransactionComparator;
import com.exscudo.peer.core.events.BlockEvent;
import com.exscudo.peer.core.events.IBlockEventListener;
import com.exscudo.peer.core.events.IPeerEventListener;
import com.exscudo.peer.core.events.PeerEvent;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.*;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.core.utils.Loggers;
import com.exscudo.peer.eon.DifficultyHelper;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.tasks.GenerateBlockTask;
import com.exscudo.peer.eon.transactions.utils.AccountDeposit;

/**
 * Creates the next block for the chain.
 */
public class BlockGenerator implements IPeerEventListener, IBlockEventListener {

	private boolean useFastGeneration = false;

	private final AtomicBoolean isGenAllowed = new AtomicBoolean();

	private final IBacklogService backlog;

	private final IBlockchainService blockchain;

	private final ITransactionHandler txHandler;

	private ISigner signer;

	public BlockGenerator(IBacklogService backlog, IBlockchainService blockchain, ITransactionHandler txHandler,
			ISigner signer) {
		this.backlog = backlog;
		this.blockchain = blockchain;
		this.txHandler = txHandler;
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

	public Block createNextBlock(Block previousBlock) {

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
			if (tx != null && !tx.isExpired(currentTimestamp) && !tx.isFuture(currentTimestamp)
					&& (tx.getReference() == 0
							|| blockchain.transactionMapper().containsTransaction(tx.getReference()))) {
				map.put(id, tx);
			}
		}

		Block parallelBlock = previousBlock;
		while (parallelBlock.getNextBlock() != 0) {
			parallelBlock = blockchain.getBlock(parallelBlock.getNextBlock());
			if (parallelBlock == null) {
				break;
			}

			for (Transaction tx : parallelBlock.getTransactions()) {

				if (!tx.isExpired(currentTimestamp) && !tx.isFuture(currentTimestamp) && (tx.getReference() == 0
						|| blockchain.transactionMapper().containsTransaction(tx.getReference()))) {
					map.put(tx.getID(), tx);
				}
			}
		}

		Transaction[] sortedTransactions = map.values().toArray(new Transaction[0]);
		Arrays.sort(sortedTransactions, new TransactionComparator());

		Map<String, Transaction> payload = new TreeMap<>();
		TransactionContext ctx = new TransactionContext(previousBlock.getTimestamp() + Constant.BLOCK_PERIOD,
				Format.MathID.pick(getSigner().getPublicKey()));

		ISandbox ledger = blockchain.getBlock(previousBlock.getID()).createSandbox(txHandler);
		int payloadLength = 0;
		for (Transaction tx : sortedTransactions) {
			int txLength = tx.getLength();
			if (txLength > EonConstant.TRANSACTION_MAX_PAYLOAD_LENGTH)
				continue;
			if (payloadLength + txLength > EonConstant.BLOCK_MAX_PAYLOAD_LENGTH)
				break;

			try {
				ledger.execute(tx, ctx);
				payload.put(Format.ID.transactionId(tx.getID()), tx);
				payloadLength += txLength;
			} catch (Exception e) {
				Loggers.info(GenerateBlockTask.class, "Excluding tr({}) from block generation payload: {}",
						Format.ID.transactionId(tx.getID()), e.getMessage());
			}
		}

		return createBlock(previousBlock, payload);

	}

	private Block createBlock(Block previousBlock, Map<String, Transaction> payload) {

		ILedger ledger = blockchain.getLastBlock().getState();
		IAccount account = ledger.getAccount(Format.MathID.pick(getSigner().getPublicKey()));
		if (account == null) {
			Loggers.warning(GenerateBlockTask.class, "Failed to set generator account.");
			return null;
		}

		int height = previousBlock.getHeight() + 1 - EonConstant.DIFFICULTY_DELAY;
		Block baseBlock = previousBlock;
		if (height > 0) {
			while (baseBlock.getHeight() > height) {
				baseBlock = blockchain.getBlock(baseBlock.getPreviousBlock());
			}
		}

		Block newBlock = new Block();
		newBlock.setVersion(1);
		newBlock.setHeight(previousBlock.getHeight() + 1);
		newBlock.setTimestamp(previousBlock.getTimestamp() + Constant.BLOCK_PERIOD);
		newBlock.setPreviousBlock(previousBlock.getID());
		newBlock.setSenderID(Format.MathID.pick(getSigner().getPublicKey()));
		newBlock.setGenerationSignature(getSigner().sign(baseBlock.getGenerationSignature()));
		newBlock.setTransactions(payload.values());
		newBlock.setSignature(getSigner().sign(newBlock.getBytes()));

		BigInteger diff;
		try {
			long generatingBalance = 0;
			AccountDeposit deposit = AccountDeposit.parse(account);
			if (deposit.getValue() >= EonConstant.MIN_DEPOSIT_SIZE
					&& (previousBlock.getHeight() - deposit.getHeight() >= Constant.BLOCK_IN_DAY
							|| deposit.getHeight() == 0)) {
				generatingBalance = deposit.getValue();
			}

			diff = DifficultyHelper.calculateDifficulty(newBlock, previousBlock, generatingBalance);
		} catch (ValidateException e) {
			return null;
		}
		newBlock.setCumulativeDifficulty(diff);

		return newBlock;
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
