package org.eontechology.and.peer.eon.midleware.parsers;

import java.util.Collection;
import java.util.Map;

import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.middleware.ILedgerAction;
import org.eontechology.and.peer.core.middleware.ITransactionParser;
import org.eontechology.and.peer.eon.midleware.Resources;
import org.eontechology.and.peer.eon.midleware.actions.ColoredCoinSupplyFireAction;
import org.eontechology.and.peer.eon.midleware.actions.ColoredCoinSupplyToAutoAction;
import org.eontechology.and.peer.eon.midleware.actions.ColoredCoinSupplyToPresetAction;
import org.eontechology.and.peer.eon.midleware.actions.EmptyAction;
import org.eontechology.and.peer.eon.midleware.actions.FeePaymentAction;
import org.eontechology.and.peer.tx.ColoredCoinID;

public class ColoredCoinSupplyParserV2 implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        Map<String, Object> data = transaction.getData();
        if (data.size() != 1) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        ILedgerAction supplyAction = new ColoredCoinSupplyToAutoAction(transaction.getSenderID());
        ILedgerAction fireAction = new ColoredCoinSupplyFireAction(transaction.getSenderID());

        if (!"auto".equals(data.get("supply"))) {

            if (!(data.get("supply") instanceof Long)) {
                throw new ValidateException(Resources.MONEY_SUPPLY_INVALID_FORMAT);
            }

            long newMoneySupply = (long) data.get("supply");
            if (newMoneySupply <= 0) {
                throw new ValidateException(Resources.MONEY_SUPPLY_OUT_OF_RANGE);
            }

            supplyAction = new ColoredCoinSupplyToPresetAction(new ColoredCoinID(transaction.getSenderID().getValue()),
                                                               newMoneySupply);
            fireAction = new EmptyAction();
        }

        return new ILedgerAction[] {
                new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()),
                supplyAction,
                fireAction
        };
    }

    @Override
    public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {
        return null;
    }
}
