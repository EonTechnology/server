package org.eontechnology.and.peer.eon.midleware.parsers;

import java.util.Collection;
import java.util.Map;

import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.crypto.CryptoProvider;
import org.eontechnology.and.peer.core.crypto.ISignature;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.eon.midleware.Resources;
import org.eontechnology.and.peer.eon.midleware.actions.FeePaymentAction;
import org.eontechnology.and.peer.eon.midleware.actions.PublicationAction;

public class PublicationParser implements ITransactionParser {

    private final ISignature signature;

    public PublicationParser() {
        this(CryptoProvider.getInstance().getSignature());
    }

    public PublicationParser(ISignature signature) {

        this.signature = signature;
    }

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        Map<String, Object> data = transaction.getData();
        if (data.size() != 1) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }
        if (!data.containsKey("seed")) {
            throw new ValidateException(Resources.SEED_NOT_SPECIFIED);
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()),
                new PublicationAction(transaction.getSenderID(), String.valueOf(data.get("seed")), signature)
        };
    }

    @Override
    public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {
        return null;
    }
}
