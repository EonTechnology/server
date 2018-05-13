package com.exscudo.peer.core.middleware;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.ledger.ILedger;

/**
 * The interface {@code ILedgerAction} provides an abstraction for
 * the action which the state of the Ledger is changed.
 */
public interface ILedgerAction {

    ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException;
}
