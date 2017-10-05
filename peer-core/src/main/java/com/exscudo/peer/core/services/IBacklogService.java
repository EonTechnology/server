package com.exscudo.peer.core.services;

import java.util.Iterator;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;

/**
 * The {@code IBacklogService} interface provides an abstraction for accessing
 * the Backlog Buffer
 * <p>
 * The Backlog Buffer stores unconfirmed transactions
 *
 */
public interface IBacklogService extends Iterable<Long> {

	/**
	 * Adds passed <code>transaction</code> to a Backlog.
	 * 
	 * @param transaction
	 *            transaction to add to the buffer. can not be null.
	 * @return true if transaction has been added to the Backlog (or already in the
	 *         block); false - otherwise.
	 * @throws ValidateException
	 *             If some property of the specified <code>transaction</code>
	 *             prevents it from being processed.
	 */
	boolean put(Transaction transaction) throws ValidateException;

	/**
	 * Find and remove transaction from Backlog.
	 * 
	 * @param id
	 *            for search
	 * @return removed transaction or null
	 */
	Transaction remove(long id);

	/**
	 * Returns the specified transaction.
	 * 
	 * @param id
	 *            for search
	 * @return transaction or null
	 */
	Transaction get(long id);

	/**
	 * Checks for the presence of transaction in the buffer.
	 * 
	 * @return true if transaction exists, otherwise -false
	 */
	default boolean contains(long id) {
		return get(id) != null;
	}

	/**
	 * Returns a list of transactions from backlog
	 */
	@Override
	Iterator<Long> iterator();

}
