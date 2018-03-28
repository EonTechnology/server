package com.exscudo.peer.eon.tx.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.actions.DepositAction;
import com.exscudo.peer.eon.ledger.actions.FeePaymentAction;
import com.exscudo.peer.eon.tx.ITransactionParser;

public class DepositParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 1) {
            throw new ValidateException("Attachment of unknown type.");
        }

        long amount;
        try {
            amount = Long.parseLong(String.valueOf(data.get("amount")));
        } catch (NumberFormatException e) {
            throw new ValidateException("Attachment of unknown type.");
        }
        if (amount < 0) {
            throw new ValidateException("Invalid amount.");
        }

        return new ILedgerAction[] {
                new DepositAction(transaction.getSenderID(), amount),
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee())
        };
    }
}
