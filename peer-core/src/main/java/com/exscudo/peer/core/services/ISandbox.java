package com.exscudo.peer.core.services;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;

/**
 * Allows you to get a list of changed properties when transiting between two
 * states (the state corresponds to the block) by sequentially executing
 * transactions.
 *
 */
public interface ISandbox {

	void execute(Transaction transaction, TransactionContext context) throws ValidateException;

	AccountProperty[] getProperties();

}
