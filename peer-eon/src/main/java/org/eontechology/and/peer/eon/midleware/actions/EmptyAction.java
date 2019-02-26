package org.eontechology.and.peer.eon.midleware.actions;

import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.middleware.ILedgerAction;
import org.eontechology.and.peer.core.middleware.LedgerActionContext;

public class EmptyAction implements ILedgerAction {

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) {
        return ledger;
    }
}
