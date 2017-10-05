package com.exscudo.peer.core.services;

import com.exscudo.peer.core.data.Transaction;

/**
 * The interface provides an abstraction for accessing transactions in blocks
 *
 */
public interface ITransactionMapper {

	boolean containsTransaction(long id);

	Transaction getTransaction(long id);
}
