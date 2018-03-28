package com.exscudo.peer.eon.tx.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.actions.FeePaymentAction;
import com.exscudo.peer.eon.ledger.actions.PaymentAction;
import com.exscudo.peer.eon.tx.ITransactionParser;

public class PaymentParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 2) {
            throw new ValidateException("Attachment of unknown type.");
        }

        long amount;
        try {
            amount = Long.parseLong(String.valueOf(data.get("amount")));
        } catch (NumberFormatException e) {
            throw new ValidateException("Attachment of unknown type. The amount format is not supports.");
        }

        if (amount <= 0) {
            throw new ValidateException("Invalid amount size.");
        }

        AccountID recipientID;
        try {
            recipientID = new AccountID(String.valueOf(data.get("recipient")));
        } catch (IllegalArgumentException e) {
            throw new ValidateException("Attachment of unknown type. The recipient format is not supports.");
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()),
                new PaymentAction(transaction.getSenderID(), amount, recipientID)
        };
    }

    @Override
    public AccountID getRecipient(Transaction transaction) {

        String value = String.valueOf(transaction.getData().get("recipient"));
        return new AccountID(value);
    }
}
