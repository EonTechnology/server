package org.eontechnology.and.peer.eon.midleware.parsers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.eon.midleware.Resources;
import org.eontechnology.and.peer.eon.midleware.actions.FeePaymentAction;
import org.eontechnology.and.peer.eon.midleware.actions.RegistrationAction;

public class RegistrationParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        Map<String, Object> data = transaction.getData();
        if (data.size() != 1) {
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
                new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()),
                registration
        };
    }

    @Override
    public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {

        HashSet<AccountID> accSet = new HashSet<>();

        for (String s : transaction.getData().keySet()) {
            try {
                accSet.add(new AccountID(s));
            } catch (Exception e) {
                throw new ValidateException(Resources.ACCOUNT_ID_INVALID_FORMAT);
            }
        }

        return accSet;
    }
}
