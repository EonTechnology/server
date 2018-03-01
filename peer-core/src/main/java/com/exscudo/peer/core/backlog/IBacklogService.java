package com.exscudo.peer.core.backlog;

import java.util.List;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.TransactionID;

/**
 * The {@code IBacklogService} interface provides an abstraction for accessing
 * the Backlog Buffer
 * <p>
 * The Backlog Buffer stores unconfirmed transactions
 */
public interface IBacklogService extends Iterable<TransactionID> {

    /**
     * Adds passed <code>transaction</code> to a Backlog.
     *
     * @param transaction transaction to add to the buffer. can not be null.
     * @return true if transaction has been added to the Backlog (or already in the
     * block); false - otherwise.
     * @throws ValidateException If some property of the specified <code>transaction</code>
     *                           prevents it from being processed.
     */
    boolean put(Transaction transaction) throws ValidateException;

    /**
     * Returns the specified transaction.
     *
     * @param id for search
     * @return transaction or null
     */
    Transaction get(TransactionID id);

    /**
     * Checks for the presence of transaction in the buffer.
     *
     * @return true if transaction exists, otherwise -false
     */
    default boolean contains(TransactionID id) {
        return get(id) != null;
    }

    /**
     * Clear Backlog.
     */
    List<Transaction> copyAndClear();

    /**
     * Returns the number of transactions in the backlog.
     *
     * @return
     */
    int size();
}
