package com.exscudo.peer.eon.tx;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.ILedgerAction;

public interface ITransactionParser {

    ILedgerAction[] parse(Transaction transaction) throws ValidateException;

    default AccountID getRecipient(Transaction transaction) {
        return null;
    }
}
