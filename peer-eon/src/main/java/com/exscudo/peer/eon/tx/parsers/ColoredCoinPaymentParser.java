package com.exscudo.peer.eon.tx.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ColoredCoinID;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.actions.ColoredCoinPaymentAction;
import com.exscudo.peer.eon.ledger.actions.FeePaymentAction;
import com.exscudo.peer.eon.tx.ITransactionParser;

public class ColoredCoinPaymentParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {
        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 3) {
            throw new ValidateException("Attachment of unknown type.");
        }

        ColoredCoinID coloredCoinID;
        try {
            coloredCoinID = new ColoredCoinID(String.valueOf(data.get("color")));
        } catch (Exception e) {
            throw new ValidateException("The 'color' field value has a unsupported format.");
        }

        long amount;
        try {
            amount = Long.parseLong(String.valueOf(data.get("amount")));
            if (amount <= 0) {
                throw new ValidateException("The 'amount' field value is out of range.");
            }
        } catch (NumberFormatException e) {
            throw new ValidateException("The 'amount' field value has a unsupported format.");
        }

        AccountID recipientID;
        try {
            recipientID = new AccountID(String.valueOf(data.get("recipient")));
        } catch (Exception e) {
            throw new ValidateException("The 'recipient' field value has a unsupported format.");
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()),
                new ColoredCoinPaymentAction(transaction.getSenderID(), amount, coloredCoinID, recipientID)
        };
    }
}
