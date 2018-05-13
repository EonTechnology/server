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

public class ColoredCoinSupplyAction implements ILedgerAction {
    private final long newMoneySupply;
    private final ColoredCoinID coinID;
    private final AccountID accountID;

    public ColoredCoinSupplyAction(ColoredCoinID coinID, long newMoneySupply) {
        this.newMoneySupply = newMoneySupply;
        this.coinID = coinID;
        this.accountID = coinID.getIssierAccount();
    }

    private void ensureValidState(ILedger ledger) throws ValidateException {
        // validate balance
        Account sender = ledger.getAccount(accountID);
        if (sender == null) {
            throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
        }
        ColoredCoinProperty senderColoredCoin = AccountProperties.getColoredCoin(sender);
        if (!senderColoredCoin.isIssued()) {
            throw new ValidateException(Resources.COLORED_COIN_NOT_EXISTS);
        }
        if (senderColoredCoin.getMoneySupply() == newMoneySupply) {
            throw new ValidateException(Resources.VALUE_ALREADY_SET);
        }
        ColoredBalanceProperty senderColoredBalance = AccountProperties.getColoredBalance(sender);
        long balance = 0;
        if (senderColoredBalance != null) {
            balance = senderColoredBalance.getBalance(coinID);
        }
        if (newMoneySupply == 0) { // detaching colored coin from account
            if (balance != senderColoredCoin.getMoneySupply()) {
                throw new ValidateException(Resources.COLORED_COIN_INCOMPLETE_MONEY_SUPPLY);
            }
        } else {
            long deltaMoneySupply = newMoneySupply - senderColoredCoin.getMoneySupply();
            if (balance + deltaMoneySupply < 0) {
                throw new ValidateException(Resources.COLORED_COIN_NOT_ENOUGH_FUNDS);
            }
        }
    }

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

        ensureValidState(ledger);

        Account sender = ledger.getAccount(accountID);
        ColoredCoinProperty coin = AccountProperties.getColoredCoin(sender);
        ColoredBalanceProperty balance = AccountProperties.getColoredBalance(sender);

        long newBalance = balance.getBalance(coinID) + (newMoneySupply - coin.getMoneySupply());

        coin.setMoneySupply(newMoneySupply);
        balance.setBalance(newBalance, coinID);

        sender = AccountProperties.setProperty(sender, coin);
        sender = AccountProperties.setProperty(sender, balance);

        return ledger.putAccount(sender);
    }
}
