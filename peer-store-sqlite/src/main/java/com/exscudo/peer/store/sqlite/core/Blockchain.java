package com.exscudo.peer.store.sqlite.core;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionMapper;
import com.exscudo.peer.core.services.IUnitOfWork;
import com.exscudo.peer.store.sqlite.Storage;
import com.exscudo.peer.store.sqlite.Storage.LockedObject;
import com.exscudo.peer.store.sqlite.merkle.Ledgers;
import com.exscudo.peer.store.sqlite.utils.BlockHelper;
import com.exscudo.peer.store.sqlite.utils.TransactionHelper;

/**
 * Basic implementation of {@code IBlockchainService} with direct connection to
 * DB
 *
 * @see IBlockchainService
 */
public class Blockchain implements IBlockchainService {
	private Storage connector;

	public Blockchain(Storage connector) {
		this.connector = connector;
	}

	@Override
	public int getBlockHeight(long id) {

		return BlockHelper.getHeight(connector.getConnection(), id);

	}

	@Override
	public long[] getLatestBlocks(int limit) {

		int height = connector.getLastBlock().getHeight();

		LockedObject lo = connector.lockBlocks();
		try {
			return BlockHelper.getBlockLinkedList(connector.getConnection(), height - limit + 1, height);
		} finally {
			lo.unlock();
		}
	}

	@Override
	public ITransactionMapper transactionMapper() {
		return new ITransactionMapper() {

			@Override
			public boolean containsTransaction(long id) {
				return TransactionHelper.contains(connector.getConnection(), id);
			}

			@Override
			public Transaction getTransaction(long id) {
				return TransactionHelper.get(connector.getConnection(), id);
			}
		};
	}

	@Override
	public IUnitOfWork beginPush(Object source, Block headBlock) {
		return connector.createUnitOfWork(headBlock);
	}

	@Override
	public Block getLastBlock() {
		return connector.getLastBlock();
	}

	@Override
	public Block getBlock(long blockID) {
		return BlockHelper.get(connector.getConnection(), blockID);
	}

	@Override
	public Block getBlockByHeight(int height) {
		return BlockHelper.getByHeight(connector.getConnection(), height);
	}

	@Override
	public ILedger getState(byte[] snapshot) {
		return Ledgers.newCachedLedger(connector.getConnection(), snapshot);
	}

}
