package org.eontechnology.and.peer.eon.midleware.actions;

import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.LedgerActionContext;
import org.eontechnology.and.peer.eon.ledger.AccountProperties;
import org.eontechnology.and.peer.eon.ledger.state.ColoredBalanceProperty;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechnology.and.peer.eon.midleware.Resources;
import org.eontechnology.and.peer.tx.ColoredCoinID;

public class ColoredCoinSupplyToPresetAction implements ILedgerAction {
    private final long newMoneySupply;
    private final ColoredCoinID coinID;
    private final AccountID accountID;

    public ColoredCoinSupplyToPresetAction(ColoredCoinID coinID, long newMoneySupply) {
        this.newMoneySupply = newMoneySupply;
        this.coinID = coinID;
        this.accountID = coinID.getIssierAccount();
    }

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

        Account account = ledger.getAccount(accountID);
        if (account == null) {
            throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
        }

        ColoredCoinProperty coin = AccountProperties.getColoredCoin(account);
        if (!coin.isIssued()) {
            throw new ValidateException(Resources.COLORED_COIN_NOT_EXISTS);
        }

        if (coin.getEmitMode() == ColoredCoinEmitMode.PRESET && coin.getMoneySupply() == newMoneySupply) {
            throw new ValidateException(Resources.VALUE_ALREADY_SET);
        }

        ColoredBalanceProperty balance = AccountProperties.getColoredBalance(account);

        long newBalance = balance.getBalance(coinID) + (newMoneySupply - coin.getMoneySupply());
        if (newBalance < 0) {
            throw new ValidateException(Resources.COLORED_COIN_NOT_ENOUGH_FUNDS);
        }

        coin.setEmitMode(ColoredCoinEmitMode.PRESET);
        coin.setMoneySupply(newMoneySupply);
        balance.setBalance(newBalance, coinID);

        account = AccountProperties.setProperty(account, coin);
        account = AccountProperties.setProperty(account, balance);

        return ledger.putAccount(account);
    }
}
