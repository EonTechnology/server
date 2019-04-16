package org.eontechnology.and.peer.core.backlog;

import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.TransactionID;

/**
 * The {@code IBacklog} interface provides an abstraction for accessing
 * the Backlog Buffer
 * <p>
 * The Backlog Buffer stores unconfirmed transactions
 */
public interface IBacklog extends Iterable<TransactionID> {

    /**
     * Adds passed <code>transaction</code> to a Backlog.
     *
     * @param transaction transaction to add to the buffer. can not be null.
     * @throws ValidateException If some property of the specified <code>transaction</code>
     *                           prevents it from being processed.
     */
    void put(Transaction transaction) throws ValidateException;

    /**
     * Returns the transaction by specified id.
     *
     * @param id for search
     * @return transaction or null
     */
    Transaction get(TransactionID id);
}
