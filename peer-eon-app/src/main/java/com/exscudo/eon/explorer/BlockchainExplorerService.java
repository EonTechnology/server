package com.exscudo.eon.explorer;

import java.io.IOException;
import java.util.*;

import com.exscudo.eon.utils.TransactionHistoryHelper;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.store.sqlite.Backlog;
import com.exscudo.peer.store.sqlite.Storage;
import com.exscudo.peer.store.sqlite.utils.BlockHelper;
import com.exscudo.peer.store.sqlite.utils.TransactionHelper;

/**
 * Account history service
 */
public class BlockchainExplorerService {
	private final Storage storage;

	private final static int LAST_BLOCK_CNT = 10;
	private final static int LAST_UNC_TRS_CNT = 10;
	private final Comparator<Block> cmprBlockDesc = new Comparator<Block>() {
		@Override
		public int compare(Block b1, Block b2) {
			return Integer.compare(b2.getHeight(), b1.getHeight());
		}
	};
	private final Comparator<Transaction> cmprUncTrs = new Comparator<Transaction>() {
		@Override
		public int compare(Transaction o1, Transaction o2) {
			return Long.compare(o2.getTimestamp(), o1.getTimestamp());
		}
	};

	public BlockchainExplorerService(Storage storage) {
		this.storage = storage;
	}

	public Collection<Transaction> getCommittedPage(String accountId, int page) throws RemotePeerException, IOException {
		return TransactionHistoryHelper.getCommittedPage(storage, accountId, page);
	}

	public Collection<Transaction> getUncommitted(String id) throws RemotePeerException, IOException {
		return TransactionHistoryHelper.getUncommitted(storage, id);
	}

	/**
	 * Get transaction of account registration
	 *
	 * @param id account ID
	 * @return Transaction of account registration if exists, else null
	 * @throws RemotePeerException
	 * @throws IOException
	 */
	public Transaction getRegTransaction(String id) throws RemotePeerException, IOException {
		long accId = getAccountId(id);
		return TransactionHelper.findRegTransaction(storage.getConnection(), accId);
	}

	public List<Block> getLastBlocks() throws RemotePeerException, IOException {
		Block lastBlock = storage.getLastBlock();
		return getLastBlocksFrom(lastBlock.getHeight());
	}

	public List<Block> getLastBlocksFrom(int height) throws RemotePeerException, IOException {
		int end = height;
		int begin = height - LAST_BLOCK_CNT  + 1;
		if (begin < 0)
			begin = 0;
		if (end < 0)
			end = 0;

		long[] blocIds = BlockHelper.getBlockLinkedList(storage.getConnection(), begin, end);
		ArrayList<Block> blocks = new ArrayList<>();
		for (int i=0; i<blocIds.length; i++){
			blocks.add(BlockHelper.get(storage.getConnection(), blocIds[i]));
		}

		Collections.sort(blocks, cmprBlockDesc);
		return blocks;
	}

	public Block getBlockByHeight(int height) throws RemotePeerException, IOException{
		return BlockHelper.getByHeight(storage.getConnection(), height);
	}

	public Block getBlockById(String blockId) throws RemotePeerException, IOException{
		long id = getBlockId(blockId);
		return BlockHelper.get(storage.getConnection(), id);
	}

	public Collection<Transaction> getTrsByBlockId(String blockId) throws RemotePeerException, IOException{
		long id = getBlockId(blockId);
		Block block = BlockHelper.get(storage.getConnection(), id);
		return block.getTransactions();
	}

	public Transaction getTransactionById(String trId) throws RemotePeerException, IOException{
		long id = getTrId(trId);
		Transaction tr = TransactionHelper.get(storage.getConnection(),id);
		if (tr != null)
			return tr;

		tr = storage.getBacklog().get(id);
		if (tr != null)
			return tr;
		return null;
	}

	public Collection<Transaction> getLastUncommittedTrs() throws RemotePeerException, IOException{
		TreeSet<Transaction> lastUncTrs = new TreeSet<>(cmprUncTrs);
		Backlog backlog = storage.getBacklog();
		for(long id : backlog){
			Transaction tr = backlog.get(id);
			if (tr == null)
				continue;
			lastUncTrs.add(tr);
			if (lastUncTrs.size() > LAST_UNC_TRS_CNT){
				lastUncTrs.pollLast();
			}
		}
		return lastUncTrs;
	}

	private long getAccountId(String accountId) throws RemotePeerException {
		try {
			return Format.ID.accountId(accountId);
		} catch (IllegalArgumentException e) {
			throw new RemotePeerException(e);
		}
	}

	private long getBlockId(String blockId) throws RemotePeerException {
		try {
			return Format.ID.blockId(blockId);
		} catch (IllegalArgumentException e) {
			throw new RemotePeerException(e);
		}
	}

	private long getTrId(String trId) throws RemotePeerException {
		try {
			return Format.ID.transactionId(trId);
		} catch (IllegalArgumentException e) {
			throw new RemotePeerException(e);
		}
	}
}
