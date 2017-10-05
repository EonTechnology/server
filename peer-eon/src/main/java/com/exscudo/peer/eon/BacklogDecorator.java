package com.exscudo.peer.eon;

import java.util.Iterator;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.*;

/**
 * Pre-validation of transactions before putting in Backlog.
 */
public class BacklogDecorator implements IBacklogService {
	private final IBacklogService backlog;
	private final IBlockchainService blockchain;

	private ITransactionHandler handler;

	public BacklogDecorator(IBacklogService backlog, IBlockchainService blockchain, ITransactionHandler handler) {
		this.backlog = backlog;
		this.blockchain = blockchain;

		this.handler = handler;
	}

	@Override
	public boolean put(Transaction transaction) throws ValidateException {

		TransactionContext context = new TransactionContext(blockchain.getLastBlock().getTimestamp());

		long accountID = transaction.getSenderID();
		ILedger state = blockchain.getLastBlock().getState();
		IAccount account = state.getAccount(accountID);

		ISandbox sandbox = blockchain.getLastBlock().createSandbox(handler);
		if (account != null) {
			Iterator<Long> indexes = backlog.iterator();
			while (indexes.hasNext()) {
				long id = indexes.next();
				Transaction tx = backlog.get(id);
				if (tx != null && tx.getSenderID() == accountID) {
					sandbox.execute(tx, context);
				}
			}
		}

		sandbox.execute(transaction, context);

		ITransactionMapper mapper = blockchain.transactionMapper();
		long id = transaction.getID();
		if (backlog.contains(id)) {

			return false;

		} else if (mapper.containsTransaction(id)) {

			return true;

		} else {

			long refID = transaction.getReference();
			if (refID != 0 && !backlog.contains(refID) && mapper.containsTransaction(refID)) {
				return false;
			}

			transaction.setHeight(0);
			backlog.put(transaction);

			return true;

		}

	}

	@Override
	public Transaction remove(long id) {
		return backlog.remove(id);
	}

	@Override
	public Transaction get(long id) {
		return backlog.get(id);
	}

	@Override
	public Iterator<Long> iterator() {
		return backlog.iterator();
	}

}
