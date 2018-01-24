package com.exscudo.peer.eon;

import java.util.Iterator;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.ITransactionMapper;
import com.exscudo.peer.core.services.TransactionContext;

/**
 * Pre-validation of transactions before putting in Backlog.
 */
public class BacklogDecorator implements IBacklogService {
	private final IBacklogService backlog;
	private final IBlockchainService blockchain;
	private final IFork fork;

	public BacklogDecorator(IBacklogService backlog, IBlockchainService blockchain, IFork fork) {
		this.backlog = backlog;
		this.blockchain = blockchain;
		this.fork = fork;
	}

	@Override
	public boolean put(Transaction transaction) throws ValidateException {

		long txID = transaction.getID();
		if (backlog.contains(txID)) {
			return false;
		}

		long accountID = transaction.getSenderID();

		Block block = blockchain.getLastBlock();
		ILedger state = blockchain.getState(block.getSnapshot());
		if (state == null) {
			throw new IllegalStateException("Can not find a ledger.");
		}
		int timestamp = block.getTimestamp() + Constant.BLOCK_PERIOD;
		ITransactionHandler handler = fork.getTransactionExecutor(timestamp);
		TransactionContext ctx = new TransactionContext(timestamp, block.getHeight() + 1);

		if (state.getAccount(accountID) != null) {
			Iterator<Long> indexes = backlog.iterator();
			while (indexes.hasNext()) {
				long id = indexes.next();
				Transaction tx = backlog.get(id);
				if (tx != null && tx.getSenderID() == accountID) {
					handler.run(tx, state, ctx);
				}
			}
		}

		handler.run(transaction, state, ctx);

		ITransactionMapper mapper = blockchain.transactionMapper();
		if (mapper.containsTransaction(txID)) {
			return true;
		}

		transaction.setHeight(0);
		backlog.put(transaction);

		return true;

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
