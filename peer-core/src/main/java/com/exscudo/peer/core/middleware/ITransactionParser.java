package com.exscudo.peer.core.middleware;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;

/**
 * The basic interface for an object that performs a format-logical control
 * of a transaction and its transformation to a set of actions.
 */
public interface ITransactionParser {

    ILedgerAction[] parse(Transaction transaction) throws ValidateException;

    AccountID getRecipient(Transaction transaction) throws ValidateException;
}
