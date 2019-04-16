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

public class ColoredCoinSupplyMaxMoneyAction implements ILedgerAction {

    private final AccountID accountID;

    public ColoredCoinSupplyMaxMoneyAction(AccountID accountID) {
        this.accountID = accountID;
    }

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

        Account account = ledger.getAccount(accountID);

        if (account == null) {
            throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
        }

        ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(account);
        if (!coloredCoin.isIssued()) {
            throw new ValidateException(Resources.COLORED_COIN_NOT_EXISTS);
        }

        if (coloredCoin.getEmitMode() == ColoredCoinEmitMode.AUTO) {

            ColoredBalanceProperty accBalances = AccountProperties.getColoredBalance(account);
            ColoredCoinID coloredCoinID = new ColoredCoinID(accountID);

            long balance = Long.MAX_VALUE - coloredCoin.getMoneySupply();

            coloredCoin.setMoneySupply(Long.MAX_VALUE);
            accBalances.setBalance(balance, coloredCoinID);

            account = AccountProperties.setProperty(account, coloredCoin);
            account = AccountProperties.setProperty(account, accBalances);

            return ledger.putAccount(account);
        }

        return ledger;
    }
}
