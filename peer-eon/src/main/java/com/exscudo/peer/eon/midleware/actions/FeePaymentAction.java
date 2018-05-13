package com.exscudo.peer.eon.midleware.actions;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.LedgerActionContext;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.midleware.Resources;

public class FeePaymentAction implements ILedgerAction {
    private final AccountID senderID;
    private final long fee;

    public FeePaymentAction(AccountID senderID, long fee) {
        this.senderID = senderID;
        this.fee = fee;
    }

    private void ensureValidState(ILedger ledger) throws ValidateException {
        Account sender = ledger.getAccount(senderID);
        if (sender == null) {
            throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
        }
        BalanceProperty balance = AccountProperties.getBalance(sender);
        if (balance.getValue() < fee) {
            throw new ValidateException(Resources.NOT_ENOUGH_FUNDS);
        }
    }

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

        ensureValidState(ledger);

        // Update sender account
        Account sender = ledger.getAccount(senderID);
        BalanceProperty newSenderBalance = AccountProperties.getBalance(sender).withdraw(fee);
        sender = AccountProperties.setProperty(sender, newSenderBalance);

        return ledger.putAccount(sender);
    }
}
