package com.exscudo.peer.eon.ledger;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.ledger.ILedger;

public interface ILedgerAction {

    ILedger run(ILedger ledger, TransactionContext context) throws ValidateException;
}
