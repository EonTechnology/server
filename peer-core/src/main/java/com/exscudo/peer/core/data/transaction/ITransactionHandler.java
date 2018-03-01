package com.exscudo.peer.core.data.transaction;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;

/**
 * The {@code ITransactionHandler} provides an abstraction of the processing of
 * transactions.
 */
public interface ITransactionHandler {

    /**
     * Processes a transaction in the specified context.
     *
     * @param transaction the transaction for processing.
     * @param ledger      account tree state
     * @param context     current context
     * @return new account tree state
     * @throws ValidateException if transaction not correct
     */
    ILedger run(Transaction transaction, ILedger ledger, TransactionContext context) throws ValidateException;
}
