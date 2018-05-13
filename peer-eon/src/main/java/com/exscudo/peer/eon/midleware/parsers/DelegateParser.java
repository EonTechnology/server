package com.exscudo.peer.eon.midleware.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.actions.DelegateAction;
import com.exscudo.peer.eon.midleware.actions.FeePaymentAction;

public class DelegateParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        final Map<String, Object> data = transaction.getData();
        if (data == null || data.size() < 1) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        DelegateAction action = new DelegateAction(transaction.getSenderID());
        for (Map.Entry<String, Object> entry : data.entrySet()) {

            AccountID id;
            try {
                id = new AccountID(entry.getKey());
            } catch (Exception e) {
                throw new ValidateException(Resources.ACCOUNT_ID_INVALID_FORMAT);
            }

            int weight;
            try {
                weight = Integer.parseInt(String.valueOf(entry.getValue()));
                if (weight < ValidationModeProperty.MIN_WEIGHT || weight > ValidationModeProperty.MAX_WEIGHT) {
                    throw new ValidateException(Resources.WEIGHT_OUT_OF_RANGE);
                }
            } catch (NumberFormatException e) {
                throw new ValidateException(Resources.WEIGHT_INVALID_FORMAT);
            }

            action.addDelegate(id, weight);
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()), action
        };
    }

    @Override
    public AccountID getRecipient(Transaction transaction) throws ValidateException {
        return null;
    }
}