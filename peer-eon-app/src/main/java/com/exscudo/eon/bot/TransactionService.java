package com.exscudo.eon.bot;

import java.io.IOException;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.LifecycleException;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.eon.ExecutionContext;

/**
 * Transaction processing service
 */
public class TransactionService {

	private final ExecutionContext context;

	public TransactionService(ExecutionContext context) {
		this.context = context;
	}

	/**
	 * Put transaction to Backlog list.
	 * 
	 * @param tx
	 * @throws RemotePeerException
	 * @throws IOException
	 */
	public void putTransaction(Transaction tx) throws RemotePeerException, IOException {
		try {
			// TODO: validate transaction
			if (tx.isFuture(context.getCurrentTime() + Constant.MAX_LATENCY)) {
				throw new LifecycleException();
			}

			context.getInstance().getBacklogService().put(tx);
		} catch (ValidateException e) {
			throw new RemotePeerException(e);
		}
	}

}
