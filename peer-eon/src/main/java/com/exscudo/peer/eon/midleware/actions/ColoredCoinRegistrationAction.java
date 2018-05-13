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

    private void ensureValidState(ILedger ledger) throws ValidateException {
        Account sender = ledger.getAccount(accountID);
        if (sender == null) {
            throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
        }
        if (AccountProperties.getColoredCoin(sender).isIssued()) {
            throw new ValidateException(Resources.COLORED_COIN_ALREADY_EXISTS);
        }
    }

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

        ensureValidState(ledger);

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

        return ledger.putAccount(account);
    }
}
