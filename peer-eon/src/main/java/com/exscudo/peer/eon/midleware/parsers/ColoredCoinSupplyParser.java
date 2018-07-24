package com.exscudo.peer.eon.midleware.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.actions.ColoredCoinSupplyAction;
import com.exscudo.peer.eon.midleware.actions.FeePaymentAction;
import com.exscudo.peer.tx.ColoredCoinID;

public class ColoredCoinSupplyParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 1) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        if (!(data.get("supply") instanceof Long)) {
            throw new ValidateException(Resources.MONEY_SUPPLY_INVALID_FORMAT);
        }

        long newMoneySupply = (long) data.get("supply");
        if (newMoneySupply < 0) {
            throw new ValidateException(Resources.MONEY_SUPPLY_OUT_OF_RANGE);
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()),
                new ColoredCoinSupplyAction(new ColoredCoinID(transaction.getSenderID().getValue()), newMoneySupply)
        };
    }

    @Override
    public AccountID getRecipient(Transaction transaction) throws ValidateException {
        return null;
    }
}
