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
import com.exscudo.peer.core.ForkProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.TransactionComparator;
import com.exscudo.peer.core.events.BlockEvent;
import com.exscudo.peer.core.events.IBlockEventListener;
import com.exscudo.peer.core.events.IPeerEventListener;
import com.exscudo.peer.core.events.PeerEvent;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.core.utils.Loggers;
import com.exscudo.peer.eon.DifficultyHelper;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.Sandbox;
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

		return createBlock(previousBlock, map.values().toArray(new Transaction[0]));

	}

	private Block createBlock(Block previousBlock, Transaction[] transactions) {

		int timestamp = previousBlock.getTimestamp() + Constant.BLOCK_PERIOD;
		int version = ForkProvider.getInstance().getBlockVersion(timestamp);
		if (version < 0) {
			return null;
		}
		long senderID = Format.MathID.pick(getSigner().getPublicKey());

		ILedger ledger = blockchain.getState(previousBlock.getSnapshot());
		IAccount generator = ledger.getAccount(senderID);
		if (generator != null) {

			int height = previousBlock.getHeight() + 1;

			AccountDeposit deposit = AccountDeposit.parse(generator);
			if (deposit.getValue() < EonConstant.MIN_DEPOSIT_SIZE
					|| (previousBlock.getHeight() - deposit.getHeight() < Constant.BLOCK_IN_DAY
							&& deposit.getHeight() != 0)) {
				Loggers.info(GenerateBlockTask.class, "Too small deposit.");
				return null;
			}

			Block targetBlock = previousBlock;
			if (height - EonConstant.DIFFICULTY_DELAY > 0) {
				int targetHeight = height - EonConstant.DIFFICULTY_DELAY;
				targetBlock = blockchain.getBlockByHeight(targetHeight);
			}
			byte[] generationSignature = getSigner().sign(targetBlock.getGenerationSignature());

			Sandbox sandbox = Sandbox.getInstance(blockchain, previousBlock);
			Arrays.sort(transactions, new TransactionComparator());
			int payloadLength = 0;
			List<Transaction> payload = new ArrayList<>(transactions.length);
			for (Transaction tx : transactions) {
				int txLength = tx.getLength();
				if (txLength > EonConstant.TRANSACTION_MAX_PAYLOAD_LENGTH)
					continue;
				if (payloadLength + txLength > EonConstant.BLOCK_MAX_PAYLOAD_LENGTH)
					break;

				try {
					sandbox.execute(tx);
					payload.add(tx);
					payloadLength += txLength;
				} catch (Exception e) {
					Loggers.info(GenerateBlockTask.class, "Excluding tr({}) from block generation payload: {}",
							Format.ID.transactionId(tx.getID()), e.getMessage());
				}
			}
			byte[] snapshot = sandbox.createSnapshot(senderID);

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

			BigInteger diff;
			try {
				diff = DifficultyHelper.calculateDifficulty(newBlock, previousBlock, deposit.getValue());
			} catch (ValidateException e) {
				return null;
			}
			newBlock.setCumulativeDifficulty(diff);

			return newBlock;

		}

		Loggers.warning(GenerateBlockTask.class, "Failed to set generator account.");
		return null;

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
