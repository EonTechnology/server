package com.exscudo.peer.eon.midleware.actions;

import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.LedgerActionContext;

public class EmptyAction implements ILedgerAction {

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) {
        return ledger;
    }
}
