package com.exscudo.peer.eon.midleware.parsers;

import java.util.Collection;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.actions.ColoredCoinRemoveAction;
import com.exscudo.peer.eon.midleware.actions.FeePaymentAction;
import com.exscudo.peer.tx.ColoredCoinID;

public class ColoredCoinRemoveParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        if (!transaction.getData().isEmpty()) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        ColoredCoinID coinID = new ColoredCoinID(transaction.getSenderID().getValue());
        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()),
                new ColoredCoinRemoveAction(coinID)
        };
    }

    @Override
    public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {
        return null;
    }
}
