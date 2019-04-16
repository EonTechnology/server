package org.eontechnology.and.peer.eon.midleware.actions;

import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.LedgerActionContext;

public class EmptyAction implements ILedgerAction {

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) {
        return ledger;
    }
}
