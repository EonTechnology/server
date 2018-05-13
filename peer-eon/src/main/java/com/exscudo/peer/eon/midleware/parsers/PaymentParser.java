package com.exscudo.peer.eon.midleware.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.actions.FeePaymentAction;
import com.exscudo.peer.eon.midleware.actions.PaymentAction;

public class PaymentParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 2) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        long amount;
        try {
            amount = Long.parseLong(String.valueOf(data.get("amount")));
            if (amount <= 0) {
                throw new ValidateException(Resources.AMOUNT_OUT_OF_RANGE);
            }
        } catch (NumberFormatException e) {
            throw new ValidateException(Resources.AMOUNT_INVALID_FORMAT);
        }

        AccountID recipientID;
        try {
            recipientID = new AccountID(String.valueOf(data.get("recipient")));
        } catch (Exception e) {
            throw new ValidateException(Resources.RECIPIENT_INVALID_FORMAT);
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()),
                new PaymentAction(transaction.getSenderID(), amount, recipientID)
        };
    }

    @Override
    public AccountID getRecipient(Transaction transaction) throws ValidateException {

        AccountID recipientID;
        try {
            recipientID = new AccountID(String.valueOf(transaction.getData().get("recipient")));
        } catch (IllegalArgumentException e) {
            throw new ValidateException(Resources.RECIPIENT_INVALID_FORMAT);
        }
        return recipientID;
    }
}
