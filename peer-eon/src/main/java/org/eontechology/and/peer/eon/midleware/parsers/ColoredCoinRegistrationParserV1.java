package org.eontechology.and.peer.eon.midleware.parsers;

import java.util.Collection;
import java.util.Map;

import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.middleware.ILedgerAction;
import org.eontechology.and.peer.core.middleware.ITransactionParser;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechology.and.peer.eon.midleware.Resources;
import org.eontechology.and.peer.eon.midleware.actions.ColoredCoinRegistrationAction;
import org.eontechology.and.peer.eon.midleware.actions.ColoredCoinSupplyToPresetAction;
import org.eontechology.and.peer.eon.midleware.actions.FeePaymentAction;
import org.eontechology.and.peer.tx.ColoredCoinID;

public class ColoredCoinRegistrationParserV1 implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        Map<String, Object> data = transaction.getData();
        if (data.size() != 2) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        if (!(data.get("emission") instanceof Long)) {
            throw new ValidateException(Resources.EMISSION_INVALID_FORMAT);
        }

        long moneySupply = (long) data.get("emission");
        if (moneySupply < ColoredCoinProperty.MIN_EMISSION_SIZE) {
            throw new ValidateException(Resources.EMISSION_OUT_OF_RANGE);
        }

        if (!(data.get("decimal") instanceof Long)) {
            throw new ValidateException(Resources.DECIMAL_POINT_INVALID_FORMAT);
        }

        long decimalPoint = (long) data.get("decimal");
        if (decimalPoint < ColoredCoinProperty.MIN_DECIMAL_POINT ||
                decimalPoint > ColoredCoinProperty.MAX_DECIMAL_POINT) {
            throw new ValidateException(Resources.DECIMAL_POINT_OUT_OF_RANGE);
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()),
                new ColoredCoinRegistrationAction(transaction.getSenderID(), (int) decimalPoint),
                new ColoredCoinSupplyToPresetAction(new ColoredCoinID(transaction.getSenderID().getValue()),
                                                    moneySupply)
        };
    }

    @Override
    public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {
        return null;
    }
}