package com.exscudo.eon.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.exscudo.eon.utils.TransactionHistoryHelper;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.store.sqlite.ConnectionProxy;
import com.exscudo.peer.store.sqlite.Storage;
import com.exscudo.peer.store.sqlite.utils.BlockHelper;

/**
 * Account history service
 */
public class TransactionHistoryService {
	private final Storage storage;

	public TransactionHistoryService(Storage state) {
		this.storage = state;
	}

	/**
	 * Get committed transactions
	 * 
	 * @param id
	 *            account ID
	 * @return
	 * @throws RemotePeerException
	 * @throws IOException
	 */
	public List<Transaction> getCommitted(String id) throws RemotePeerException, IOException {
		return getCommittedPage(id, 0);
	}

	/**
	 * Get committed transactions
	 *
	 * @param id
	 *            account ID
	 * @param page
	 *            page number
	 * @return
	 * @throws RemotePeerException
	 * @throws IOException
	 */
	public List<Transaction> getCommittedPage(String id, int page) throws RemotePeerException, IOException {
		return TransactionHistoryHelper.getCommittedPage(storage, id, page);
	}

	/**
	 * Get uncommitted transactions
	 * 
	 * @param id
	 *            account ID
	 * @return
	 * @throws RemotePeerException
	 * @throws IOException
	 *
	 * @see IBacklogService
	 */
	public List<Transaction> getUncommitted(String id) throws RemotePeerException, IOException {
		return TransactionHistoryHelper.getUncommitted(storage, id);
	}

	/**
	 * Get committed blocks
	 * 
	 * @param id
	 *            account ID
	 * @return
	 * @throws RemotePeerException
	 * @throws IOException
	 */
	public List<Block> getSignedBlock(String id) throws RemotePeerException, IOException {
		List<Block> items = new ArrayList<>();
		ConnectionProxy connection = storage.getConnection();

		long accID = TransactionHistoryHelper.getAccountId(id);

		Block lastBlock = this.storage.getLastBlock();

		long[] list = BlockHelper.getBlockListByPeerAccount(connection, accID,
				lastBlock.getTimestamp() - (24 * 60 * 60));
		for (long item : list) {
			Block b = BlockHelper.get(connection, item);

			if (b != null) {
				// Clear transactions in block ti minimize package size
				// b.setTransactions(new ArrayList<>());
				items.add(b);
			}
		}

		return items;
	}


}
