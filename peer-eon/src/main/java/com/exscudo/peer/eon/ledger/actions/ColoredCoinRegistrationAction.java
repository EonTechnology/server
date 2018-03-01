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

public class ColoredCoinRegistrationAction implements ILedgerAction {
    private final int decimalPoint;
    private final long moneySupply;
    private final AccountID accountID;
    private final ColoredCoinID coinID;

    public ColoredCoinRegistrationAction(AccountID accountID, long moneySupply, int decimalPoint) {
        this.decimalPoint = decimalPoint;
        this.coinID = new ColoredCoinID(accountID.getValue());
        this.accountID = accountID;
        this.moneySupply = moneySupply;
    }

    private ValidationResult canRun(ILedger ledger) {
        Account sender = ledger.getAccount(accountID);
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }
        if (AccountProperties.getColoredCoin(sender).isIssued()) {
            return ValidationResult.error("Account is already associated with a color coin.");
        }

        return ValidationResult.success;
    }

    @Override
    public ILedger run(ILedger ledger, TransactionContext context) throws ValidateException {
        ValidationResult r = canRun(ledger);
        if (r.hasError) {
            throw r.cause;
        }

        Account account = ledger.getAccount(accountID);

        // Setup colored coin info
        ColoredCoinProperty coloredCoin = new ColoredCoinProperty();
        coloredCoin.setMoneySupply(moneySupply);
        coloredCoin.setDecimalPoint(decimalPoint);
        // Sets only on money creation
        coloredCoin.setTimestamp(context.getTimestamp());

        account = AccountProperties.setProperty(account, coloredCoin);

        // Update colored coin balances
        ColoredBalanceProperty coloredBalance = AccountProperties.getColoredBalance(account);
        coloredBalance.setBalance(moneySupply, coinID);
        account = AccountProperties.setProperty(account, coloredBalance);

        ILedger newLedger = ledger.putAccount(account);

        return newLedger;
    }
}
