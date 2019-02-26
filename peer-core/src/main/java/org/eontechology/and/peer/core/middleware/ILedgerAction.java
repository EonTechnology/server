package org.eontechology.and.peer.core.middleware;

import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.ledger.ILedger;

/**
 * The interface {@code ILedgerAction} provides an abstraction for
 * the action which the state of the Ledger is changed.
 */
public interface ILedgerAction {

    ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException;
}
