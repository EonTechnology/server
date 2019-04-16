package org.eontechnology.and.peer.eon.midleware.parsers;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.eon.midleware.Resources;
import org.eontechnology.and.peer.eon.midleware.actions.ColoredCoinPaymentAction;
import org.eontechnology.and.peer.eon.midleware.actions.ColoredCoinSupplyFireAction;
import org.eontechnology.and.peer.eon.midleware.actions.ColoredCoinSupplyMaxMoneyAction;
import org.eontechnology.and.peer.eon.midleware.actions.EmptyAction;
import org.eontechnology.and.peer.eon.midleware.actions.FeePaymentAction;
import org.eontechnology.and.peer.tx.ColoredCoinID;

public class ColoredCoinPaymentParser implements ITransactionParser {

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions() != null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
        }

        Map<String, Object> data = transaction.getData();
        if (data.size() != 3) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        ColoredCoinID coloredCoinID;
        try {
            coloredCoinID = new ColoredCoinID(String.valueOf(data.get("color")));
        } catch (Exception e) {
            throw new ValidateException(Resources.COLOR_INVALID_FORMAT);
        }

        if (!(data.get("amount") instanceof Long)) {
            throw new ValidateException(Resources.AMOUNT_INVALID_FORMAT);
        }

        long amount = (long) data.get("amount");
        if (amount <= 0) {
            throw new ValidateException(Resources.AMOUNT_OUT_OF_RANGE);
        }

        AccountID recipientID;
        try {
            recipientID = new AccountID(String.valueOf(data.get("recipient")));
        } catch (Exception e) {
            throw new ValidateException(Resources.RECIPIENT_INVALID_FORMAT);
        }

        ILedgerAction emissionAction = new EmptyAction();
        ILedgerAction fireAction = new EmptyAction();

        AccountID issuerAccount = coloredCoinID.getIssierAccount();
        if (transaction.getSenderID().equals(issuerAccount)) {
            emissionAction = new ColoredCoinSupplyMaxMoneyAction(issuerAccount);
            fireAction = new ColoredCoinSupplyFireAction(issuerAccount);
        } else if (recipientID.equals(issuerAccount)) {
            fireAction = new ColoredCoinSupplyFireAction(issuerAccount);
        }

        return new ILedgerAction[] {
                emissionAction,
                new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()),
                new ColoredCoinPaymentAction(transaction.getSenderID(), amount, coloredCoinID, recipientID),
                fireAction
        };
    }

    @Override
    public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {

        AccountID id;
        try {
            id = new AccountID(transaction.getData().get("recipient").toString());
        } catch (Exception e) {
            throw new ValidateException(Resources.RECIPIENT_INVALID_FORMAT);
        }
        return Collections.singleton(id);
    }
}
