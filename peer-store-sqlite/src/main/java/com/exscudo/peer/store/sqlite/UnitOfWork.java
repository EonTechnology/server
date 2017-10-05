package com.exscudo.peer.store.sqlite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.IUnitOfWork;
import com.exscudo.peer.core.services.LinkedBlock;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.store.sqlite.core.LinkedBlockImpl;
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
	private LedgerBase ledger;

	private Block headBlock;

	private ISegment<Transaction> backlog = new ISegment<Transaction>() {

		List<Long> removed = new ArrayList<>();
		HashMap<Long, Transaction> saved = new HashMap<>();

		@Override
		public Transaction get(long id) {

			if (removed.contains(id)) {
				return null;
			}

			if (saved.containsKey(id)) {
				return saved.get(id);
			}

			return connector.getBacklog().get(id);
		}

		@Override
		public void put(Transaction instance) {
			long id = instance.getID();
			saved.put(id, instance);
			removed.remove(id);
		}

		@Override
		public void remove(long id) {
			saved.remove(id);
			removed.add(id);
		}

		@Override
		public boolean contains(long id) {
			return get(id) != null;
		}

		@Override
		public void commit() {
			for (Long id : removed) {
				connector.getBacklog().remove(id);
			}
			for (Map.Entry<Long, Transaction> entry : saved.entrySet()) {
				connector.getBacklog().put(entry.getValue());
			}
		}

	};

	private ISegment<Block> blockSet = new ISegment<Block>() {

		List<Long> removed = new ArrayList<>();
		HashMap<Long, Block> saved = new HashMap<>();

		@Override
		public Block get(long id) {
			if (removed.contains(id)) {
				return null;
			}

			if (saved.containsKey(id)) {
				return saved.get(id);
			}

			return BlockHelper.get(connection, id);
		}

		@Override
		public void put(Block instance) {
			long id = instance.getID();
			saved.put(id, instance);
			removed.remove(id);
		}

		@Override
		public void remove(long id) {
			saved.remove(id);
			removed.add(id);
		}

		@Override
		public boolean contains(long id) {
			return get(id) != null;
		}

		@Override
		public void commit() {
			for (Long id : removed) {
				BlockHelper.remove(connection, id);
			}
			for (Map.Entry<Long, Block> entry : saved.entrySet()) {
				BlockHelper.save(connection, entry.getValue());
			}
		}

	};

	private ISegment<Transaction> transactionSet = new ISegment<Transaction>() {

		List<Long> removed = new ArrayList<>();
		HashMap<Long, Transaction> saved = new HashMap<>();

		@Override
		public Transaction get(long id) {

			if (removed.contains(id)) {
				return null;
			}

			if (saved.containsKey(id)) {
				return saved.get(id);
			}

			return TransactionHelper.get(connection, id);
		}

		@Override
		public void put(Transaction instance) {
			long id = instance.getID();
			saved.put(id, instance);
			removed.remove(id);
		}

		@Override
		public void remove(long id) {
			saved.remove(id);
			removed.add(id);
		}

		@Override
		public boolean contains(long id) {
			return get(id) != null;
		}

		@Override
		public void commit() {
			for (Long id : removed) {
				TransactionHelper.remove(connection, id);
			}
			for (Map.Entry<Long, Transaction> entry : saved.entrySet()) {
				TransactionHelper.save(connection, entry.getValue());
			}
		}

	};

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
		ledger = new LedgerState(connector, headBlock);
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
		blockSet.put(block);

		headBlock = block;
	}

	private Block pop(Block block) {

		Block removedBlock = blockSet.get(block.getID());

		for (Transaction tx : removedBlock.getTransactions()) {

			tx.setBlock(0);
			tx.setHeight(0);
			backlog.put(tx);
			transactionSet.remove(tx.getID());
		}

		blockSet.remove(block.getID());

		return removedBlock;
	}

	@Override
	public void commit() {

		blockSet.commit();
		transactionSet.commit();
		backlog.commit();

		DatabaseHelper.commitTransaction(connection, TRAN_NAME);

		connector.setLastBlock(headBlock);

		backlogRoot.unlock();
		txRoot.unlock();
		blockRoot.unlock();
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

		Block prevBlock = headBlock;
		if (prevBlock.getID() != newBlock.getPreviousBlock()) {
			throw new IllegalStateException(
					"Unexpected block. Expected - " + Format.ID.blockId(newBlock.getPreviousBlock()) + ", current - "
							+ Format.ID.blockId(prevBlock.getID()));
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

			transactionSet.put(tx);
		}

		prevBlock.setNextBlock(id);

		blockSet.put(prevBlock);
		blockSet.put(newBlock);

		headBlock = blockSet.get(id);
		ledger = new LedgerProxy(headBlock, ledger);
		return headBlock;
	}

	@Override
	public LinkedBlock getLastBlock() {
		return new LinkedBlockImpl(ledger, headBlock);
	}

	@Override
	public LinkedBlock getBlock(long blockID) {

		return new LinkedBlockImpl(null, blockSet.get(blockID)) {
			private static final long serialVersionUID = 1L;

			@Override
			public ILedger getState() {
				throw new UnsupportedOperationException();
			}

		};

	}

}
