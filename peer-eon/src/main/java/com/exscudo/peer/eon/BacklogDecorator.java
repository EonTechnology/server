package com.exscudo.peer.eon;

import java.util.Iterator;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.services.ITransactionMapper;

/**
 * Pre-validation of transactions before putting in Backlog.
 */
public class BacklogDecorator implements IBacklogService {
	private final IBacklogService backlog;
	private final IBlockchainService blockchain;

	public BacklogDecorator(IBacklogService backlog, IBlockchainService blockchain) {
		this.backlog = backlog;
		this.blockchain = blockchain;
	}

	@Override
	public boolean put(Transaction transaction) throws ValidateException {

		long accountID = transaction.getSenderID();

		Sandbox sandbox = Sandbox.getInstance(blockchain);
		IAccount account = sandbox.getLedger().getAccount(accountID);
		if (account != null) {
			Iterator<Long> indexes = backlog.iterator();
			while (indexes.hasNext()) {
				long id = indexes.next();
				Transaction tx = backlog.get(id);
				if (tx != null && tx.getSenderID() == accountID) {
					sandbox.execute(tx);
				}
			}
		}

		sandbox.execute(transaction);

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
