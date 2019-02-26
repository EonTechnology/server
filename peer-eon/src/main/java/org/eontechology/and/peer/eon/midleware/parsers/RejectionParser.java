package org.eontechology.and.peer.eon.midleware.parsers;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.middleware.ILedgerAction;
import org.eontechology.and.peer.core.middleware.ITransactionParser;
import org.eontechology.and.peer.eon.midleware.Resources;
import org.eontechology.and.peer.eon.midleware.actions.FeePaymentAction;
import org.eontechology.and.peer.eon.midleware.actions.RejectionAction;

public class RejectionParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        Map<String, Object> data = transaction.getData();
        if (data.size() != 1) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        AccountID id;
        try {
            id = new AccountID(String.valueOf(data.get("account")));
        } catch (Exception e) {
            throw new ValidateException(Resources.ACCOUNT_ID_INVALID_FORMAT);
        }
        if (id.equals(transaction.getSenderID())) {
            throw new ValidateException(Resources.ACCOUNT_ID_NOT_MATCH_DATA);
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()),
                new RejectionAction(transaction.getSenderID(), id)
        };
    }

    @Override
    public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {
        AccountID id;
        try {
            id = new AccountID(transaction.getData().get("account").toString());
        } catch (Exception e) {
            throw new ValidateException(Resources.ACCOUNT_ID_INVALID_FORMAT);
        }
        return Collections.singleton(id);
    }
}
