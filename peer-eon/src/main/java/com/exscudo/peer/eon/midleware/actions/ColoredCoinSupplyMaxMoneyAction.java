package com.exscudo.peer.eon.midleware.actions;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.LedgerActionContext;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredCoinEmitMode;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.tx.ColoredCoinID;

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
