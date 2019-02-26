package org.eontechology.and.peer.core.middleware;

import java.util.Collection;

import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;

/**
 * The basic interface for an object that performs a format-logical control
 * of a transaction and its transformation to a set of actions.
 */
public interface ITransactionParser {

    ILedgerAction[] parse(Transaction transaction) throws ValidateException;

    Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException;
}
