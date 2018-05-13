package com.exscudo.peer.eon.midleware.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.actions.FeePaymentAction;
import com.exscudo.peer.eon.midleware.actions.RegistrationAction;

public class RegistrationParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 1) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        RegistrationAction registration = new RegistrationAction();
        for (Map.Entry<String, Object> entry : data.entrySet()) {

            AccountID id;
            try {
                id = new AccountID(entry.getKey());
            } catch (Exception e) {
                throw new ValidateException(Resources.ACCOUNT_ID_INVALID_FORMAT);
            }

            byte[] publicKey;
            try {
                publicKey = Format.convert(String.valueOf(entry.getValue()));
                if (publicKey.length != 32) {
                    throw new IllegalArgumentException();
                }
                if (!id.equals(new AccountID(publicKey))) {
                    throw new IllegalArgumentException();
                }
            } catch (Exception e) {
                throw new ValidateException(Resources.ACCOUNT_ID_NOT_MATCH_DATA);
            }

            registration.addAccount(id, publicKey);
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()), registration
        };
    }

    @Override
    public AccountID getRecipient(Transaction transaction) throws ValidateException {

        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 1) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        AccountID id;
        try {
            id = new AccountID(data.keySet().iterator().next());
        } catch (Exception e) {
            throw new ValidateException(Resources.ACCOUNT_ID_INVALID_FORMAT);
        }

        return id;
    }
}
