package com.exscudo.peer.eon.ledger.actions;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ColoredCoinID;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;

public class ColoredCoinPaymentAction implements ILedgerAction {
    private final AccountID recipientID;
    private final AccountID senderID;
    private final long amount;
    private final ColoredCoinID coinID;

    public ColoredCoinPaymentAction(AccountID senderID, long amount, ColoredCoinID coinID, AccountID recipientID) {
        this.recipientID = recipientID;
        this.senderID = senderID;
        this.amount = amount;
        this.coinID = coinID;
    }

    private ValidationResult canRun(ILedger ledger) {

        // validate color
        Account coinAccount = ledger.getAccount(coinID.getIssierAccount());
        if (coinAccount == null) {
            return ValidationResult.error("Unknown colored coin.");
        }

        ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(coinAccount);
        if (!coloredCoin.isIssued()) {
            return ValidationResult.error("Account is not associated with a colored coin.");
        }

        // check available funds on the balance of the sender
        Account sender = ledger.getAccount(senderID);
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }
        ColoredBalanceProperty senderColoredBalance = AccountProperties.getColoredBalance(sender);
        if (amount > senderColoredBalance.getBalance(coinID)) {
            return ValidationResult.error("Insufficient funds.");
        }

        // check existence of the recipient
        Account recipient = ledger.getAccount(recipientID);
        if (recipient == null) {
            return ValidationResult.error("Unknown recipient.");
        }

        return ValidationResult.success;
    }

    @Override
    public ILedger run(ILedger ledger, TransactionContext context) throws ValidateException {
        ValidationResult r = canRun(ledger);
        if (r.hasError) {
            throw r.cause;
        }

        ILedger newLedger = ledger;

        // Update sender account
        Account sender = newLedger.getAccount(senderID);
        ColoredBalanceProperty senderColoredBalance = AccountProperties.getColoredBalance(sender);
        senderColoredBalance.withdraw(amount, coinID);

        sender = AccountProperties.setProperty(sender, senderColoredBalance);
        newLedger = newLedger.putAccount(sender);

        // Update recipient account
        Account recipient = newLedger.getAccount(recipientID);
        ColoredBalanceProperty recipientColoredBalance = AccountProperties.getColoredBalance(recipient);
        recipientColoredBalance.refill(amount, coinID);

        recipient = AccountProperties.setProperty(recipient, recipientColoredBalance);
        newLedger = newLedger.putAccount(recipient);

        return newLedger;
    }
}
