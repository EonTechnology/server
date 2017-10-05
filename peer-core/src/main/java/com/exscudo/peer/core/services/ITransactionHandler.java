package com.exscudo.peer.core.services;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;

/**
 * The {@code ITransactionHandler} provides an abstraction of the processing of
 * transactions.
 *
 */
public interface ITransactionHandler {

	/**
	 * Processes a transaction in the specified context.
	 * 
	 * @param transaction
	 *            the transaction for processing.
	 * @param context
	 * @throws ValidateException
	 *             if some property of the specified {@code transaction} prevents it
	 *             from being processed.
	 */
	void run(Transaction transaction, ILedger ledger, TransactionContext context) throws ValidateException;

}
