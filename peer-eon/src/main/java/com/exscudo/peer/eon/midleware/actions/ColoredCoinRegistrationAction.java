package com.exscudo.peer.eon.midleware.actions;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.LedgerActionContext;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.ColoredCoinEmitMode;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.midleware.Resources;

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
