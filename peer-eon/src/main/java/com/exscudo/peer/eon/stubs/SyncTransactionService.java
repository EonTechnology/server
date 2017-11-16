package com.exscudo.peer.eon.stubs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.services.ITransactionSynchronizationService;

/**
 * Basic implementation of the {@code ITransactionSynchronizationService}
 * interface
 *
 */
public class SyncTransactionService extends BaseService implements ITransactionSynchronizationService {
	/**
	 * The maximum number of transactions transmitted during synchronization.
	 */
	private static final int TRANSACTION_LIMIT = 100;

	private final ExecutionContext context;

	public SyncTransactionService(ExecutionContext context) {
		this.context = context;
	}

	@Override
	public Transaction[] getTransactions(String lastBlockId, String[] ignoreList)
			throws RemotePeerException, IOException {

		List<Long> ignoreIDs = new ArrayList<Long>();
		try {

			if (context.getInstance().getBlockchainService().getLastBlock().getID() != Format.ID.blockId(lastBlockId)
					&& !context.isCurrentForkPassed()) {
				return new Transaction[0];
			}

			for (String encodedID : ignoreList) {
				ignoreIDs.add(Format.ID.transactionId(encodedID));
			}
		} catch (IllegalArgumentException e) {
			throw new RemotePeerException("Unsupported request. Invalid transaction ID format.", e);
		}

		try {

			List<Transaction> list = new ArrayList<Transaction>();

			int blockSize = EonConstant.BLOCK_TRANSACTION_LIMIT;

			IBacklogService backlog = context.getInstance().getBacklogService();
			final Iterator<Long> indexes = backlog.iterator();
			while (indexes.hasNext() && list.size() < TRANSACTION_LIMIT && blockSize > 0) {

				Long id = indexes.next();

				if (!ignoreIDs.contains(id)) {
					Transaction tx = backlog.get(id);
					if (tx != null) {
						list.add(tx);
					}
				}

				blockSize--;
			}
			return list.toArray(new Transaction[0]);

		} catch (Exception e) {
			throw new IOException("Failed to get the transaction list.", e);
		}
	}
}
