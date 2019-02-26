package org.eontechology.and.peer.eon.midleware.actions;

import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.middleware.ILedgerAction;
import org.eontechology.and.peer.core.middleware.LedgerActionContext;
import org.eontechology.and.peer.eon.ledger.AccountProperties;
import org.eontechology.and.peer.eon.ledger.state.ColoredBalanceProperty;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechology.and.peer.eon.midleware.Resources;
import org.eontechology.and.peer.tx.ColoredCoinID;

public class ColoredCoinRemoveAction implements ILedgerAction {
    private final ColoredCoinID coinID;
    private final AccountID accountID;

    public ColoredCoinRemoveAction(ColoredCoinID coinID) {
        this.coinID = coinID;
        this.accountID = coinID.getIssierAccount();
    }

    private void ensureValidState(ILedger ledger) throws ValidateException {

        Account sender = ledger.getAccount(accountID);
        if (sender == null) {
            throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
        }
        ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(sender);
        if (!coloredCoin.isIssued()) {
            throw new ValidateException(Resources.COLORED_COIN_NOT_EXISTS);
        }

        ColoredCoinEmitMode emitMode = coloredCoin.getEmitMode();
        if (emitMode == ColoredCoinEmitMode.AUTO) {

            if (coloredCoin.getMoneySupply() != 0L) {
                throw new ValidateException(Resources.COLORED_COIN_INCOMPLETE_MONEY_SUPPLY);
            }
        } else if (emitMode == ColoredCoinEmitMode.PRESET) {

            ColoredBalanceProperty coloredBalance = AccountProperties.getColoredBalance(sender);
            long balance = coloredBalance.getBalance(coinID);
            if (balance != coloredCoin.getMoneySupply()) {
                throw new ValidateException(Resources.COLORED_COIN_INCOMPLETE_MONEY_SUPPLY);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {
        ensureValidState(ledger);

        Account sender = ledger.getAccount(accountID);

        ColoredBalanceProperty balance = AccountProperties.getColoredBalance(sender);
        balance.setBalance(0, coinID);
        sender = AccountProperties.setProperty(sender, balance);

        ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(sender);
        coloredCoin.setAttributes(null);
        sender = AccountProperties.setProperty(sender, coloredCoin);

        return ledger.putAccount(sender);
    }
}
