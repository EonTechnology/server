package com.exscudo.peer.eon.tx.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.actions.DelegateAction;
import com.exscudo.peer.eon.ledger.actions.FeePaymentAction;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.tx.ITransactionParser;

public class DelegateParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        final Map<String, Object> data = transaction.getData();
        if (data == null || data.size() < 1) {
            throw new ValidateException("Attachment of unknown type.");
        }

        DelegateAction action = new DelegateAction(transaction.getSenderID());
        for (Map.Entry<String, Object> entry : data.entrySet()) {

            AccountID id;
            int weight;
            try {
                id = new AccountID(entry.getKey());
                weight = Integer.parseInt(String.valueOf(entry.getValue()));
            } catch (Exception e) {
                throw new ValidateException("Attachment of unknown type.");
            }
            if (weight < ValidationModeProperty.MIN_WEIGHT || weight > ValidationModeProperty.MAX_WEIGHT) {
                throw new ValidateException("Invalid " + entry.getKey() + " account weight.");
            }
            action.addDelegate(id, weight);
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()), action
        };
    }
}
