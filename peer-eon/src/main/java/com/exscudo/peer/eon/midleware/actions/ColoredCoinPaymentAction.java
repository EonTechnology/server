package com.exscudo.peer.eon.midleware.actions;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.LedgerActionContext;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.tx.ColoredCoinID;

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

    private void ensureValidState(ILedger ledger) throws ValidateException {

        // validate color
        Account coinAccount = ledger.getAccount(coinID.getIssierAccount());
        if (coinAccount == null) {
            throw new ValidateException(Resources.COLORED_COIN_ACCOUNT_NOT_FOUND);
        }

        ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(coinAccount);
        if (!coloredCoin.isIssued()) {
            throw new ValidateException(Resources.COLORED_COIN_NOT_EXISTS);
        }

        // check available funds on the balance of the sender
        Account sender = ledger.getAccount(senderID);
        if (sender == null) {
            throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
        }
        ColoredBalanceProperty senderColoredBalance = AccountProperties.getColoredBalance(sender);
        if (amount > senderColoredBalance.getBalance(coinID)) {
            throw new ValidateException(Resources.COLORED_COIN_NOT_ENOUGH_FUNDS);
        }

        // check existence of the recipient
        Account recipient = ledger.getAccount(recipientID);
        if (recipient == null) {
            throw new ValidateException(Resources.RECIPIENT_ACCOUNT_NOT_FOUND);
        }
        ColoredBalanceProperty recipientColoredBalance = AccountProperties.getColoredBalance(recipient);
        if (recipientColoredBalance.isFull() && !recipientColoredBalance.containBalance(coinID)) {
            throw new ValidateException(Resources.TOO_MACH_SIZE);
        }
    }

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

        ensureValidState(ledger);

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
