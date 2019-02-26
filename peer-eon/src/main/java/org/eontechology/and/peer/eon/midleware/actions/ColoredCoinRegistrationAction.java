package org.eontechology.and.peer.eon.midleware.actions;

import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.middleware.ILedgerAction;
import org.eontechology.and.peer.core.middleware.LedgerActionContext;
import org.eontechology.and.peer.eon.ledger.AccountProperties;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechology.and.peer.eon.midleware.Resources;

public class ColoredCoinRegistrationAction implements ILedgerAction {
    private final int decimalPoint;
    private final AccountID accountID;

    public ColoredCoinRegistrationAction(AccountID accountID, int decimalPoint) {
        this.decimalPoint = decimalPoint;
        this.accountID = accountID;
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

        ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(account);
        // Sets only on money creation
        coloredCoin.setAttributes(new ColoredCoinProperty.Attributes(decimalPoint, context.getTimestamp()));
        coloredCoin.setEmitMode(ColoredCoinEmitMode.AUTO);

        account = AccountProperties.setProperty(account, coloredCoin);

        return ledger.putAccount(account);
    }
}
