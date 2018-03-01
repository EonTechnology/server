package com.exscudo.peer.eon.tx.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.actions.FeePaymentAction;
import com.exscudo.peer.eon.ledger.actions.RejectionAction;
import com.exscudo.peer.eon.tx.ITransactionParser;

public class RejectionParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 1) {
            throw new ValidateException("Attachment of unknown type.");
        }

        AccountID id;
        try {
            id = new AccountID(String.valueOf(data.get("account")));
        } catch (Exception e) {
            throw new ValidateException("Attachment of unknown type.");
        }
        if (id.equals(transaction.getSenderID())) {
            throw new ValidateException("Illegal account.");
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()),
                new RejectionAction(transaction.getSenderID(), id)
        };
    }
}
