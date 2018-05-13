package com.exscudo.peer.eon.midleware.parsers;

import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.actions.ColoredCoinRegistrationAction;
import com.exscudo.peer.eon.midleware.actions.FeePaymentAction;

public class ColoredCoinRegistrationParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        Map<String, Object> data = transaction.getData();
        if (data == null || data.size() != 2) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        long moneySupply;
        try {
            moneySupply = Long.parseLong(String.valueOf(data.get("emission")));
            if (moneySupply < ColoredCoinProperty.MIN_EMISSION_SIZE) {
                throw new ValidateException(Resources.EMISSION_OUT_OF_RANGE);
            }
        } catch (NumberFormatException e) {
            throw new ValidateException(Resources.EMISSION_INVALID_FORMAT);
        }

        int decimalPoint;
        try {
            decimalPoint = Integer.parseInt(String.valueOf(data.get("decimal")));
            if (decimalPoint < ColoredCoinProperty.MIN_DECIMAL_POINT ||
                    decimalPoint > ColoredCoinProperty.MAX_DECIMAL_POINT) {
                throw new ValidateException(Resources.DECIMAL_POINT_OUT_OF_RANGE);
            }
        } catch (NumberFormatException e) {
            throw new ValidateException(Resources.DECIMAL_POINT_INVALID_FORMAT);
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getFee()),
                new ColoredCoinRegistrationAction(transaction.getSenderID(), moneySupply, decimalPoint)
        };
    }

    @Override
    public AccountID getRecipient(Transaction transaction) throws ValidateException {
        return null;
    }
}
