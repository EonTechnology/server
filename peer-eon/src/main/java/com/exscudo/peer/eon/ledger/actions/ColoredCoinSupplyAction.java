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

public class ColoredCoinSupplyAction implements ILedgerAction {
    private final long newMoneySupply;
    private final ColoredCoinID coinID;
    private final AccountID accountID;

    public ColoredCoinSupplyAction(ColoredCoinID coinID, long newMoneySupply) {
        this.newMoneySupply = newMoneySupply;
        this.coinID = coinID;
        this.accountID = coinID.getIssierAccount();
    }

    private ValidationResult canRun(ILedger ledger) {
        // validate balance
        Account sender = ledger.getAccount(accountID);
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }
        ColoredCoinProperty senderColoredCoin = AccountProperties.getColoredCoin(sender);
        if (!senderColoredCoin.isIssued()) {
            return ValidationResult.error("Colored coin is not associated with an account.");
        }
        if (senderColoredCoin.getMoneySupply() == newMoneySupply) {
            return ValidationResult.error("Value already set.");
        }
        ColoredBalanceProperty senderColoredBalance = AccountProperties.getColoredBalance(sender);
        long balance = 0;
        if (senderColoredBalance != null) {
            balance = senderColoredBalance.getBalance(coinID);
        }
        if (newMoneySupply == 0) { // detaching colored coin from account
            if (balance != senderColoredCoin.getMoneySupply()) {
                return ValidationResult.error("The entire amount of funds must be on the balance.");
            }
        } else {
            long deltaMoneySupply = newMoneySupply - senderColoredCoin.getMoneySupply();
            if (balance + deltaMoneySupply < 0) {
                return ValidationResult.error("Insufficient number of colored coins on the balance.");
            }
        }

        return ValidationResult.success;
    }

    @Override
    public ILedger run(ILedger ledger, TransactionContext context) throws ValidateException {
        ValidationResult r = canRun(ledger);
        if (r.hasError) {
            throw r.cause;
        }

        Account sender = ledger.getAccount(accountID);
        ColoredCoinProperty coin = AccountProperties.getColoredCoin(sender);
        ColoredBalanceProperty balance = AccountProperties.getColoredBalance(sender);

        long newBalance = balance.getBalance(coinID) + (newMoneySupply - coin.getMoneySupply());

        coin.setMoneySupply(newMoneySupply);
        balance.setBalance(newBalance, coinID);

        sender = AccountProperties.setProperty(sender, coin);
        sender = AccountProperties.setProperty(sender, balance);
        ILedger newLedger = ledger.putAccount(sender);

        return newLedger;
    }
}
