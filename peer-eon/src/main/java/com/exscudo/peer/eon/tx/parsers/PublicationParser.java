package com.exscudo.peer.eon.tx.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.actions.FeePaymentAction;
import com.exscudo.peer.eon.ledger.actions.PublicationAction;
import com.exscudo.peer.eon.tx.ITransactionParser;

public class PublicationParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 1) {
            throw new ValidateException("Attachment of unknown type.");
        }
        if (!data.containsKey("seed")) {
            throw new ValidateException("The 'seed' field is not specified.");
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()),
                new PublicationAction(transaction.getSenderID(), String.valueOf(data.get("seed")))
        };
    }
}
