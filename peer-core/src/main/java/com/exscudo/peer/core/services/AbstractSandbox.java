package com.exscudo.peer.core.services;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;

/**
 * The simplest abstract class which implementation {@code ISandbox} interface.
 *
 * @see ISandbox
 */
public abstract class AbstractSandbox implements ISandbox {
	protected final ITransactionHandler handler;

	protected AbstractSandbox(ITransactionHandler handler) {
		this.handler = handler;
	}

	protected abstract ILedger getLedger();

	@Override
	public void execute(Transaction transaction, TransactionContext context) throws ValidateException {
		handler.run(transaction, getLedger(), context);
	}

}
