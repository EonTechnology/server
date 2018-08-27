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
    private final AccountID payerID;
    private final long fee;

    public FeePaymentAction(AccountID senderID, AccountID payerID, long fee) {
        this.senderID = senderID;
        this.payerID = payerID;
        this.fee = fee;
    }

    private void ensureValidState(ILedger ledger) throws ValidateException {
        Account payer;
        if (payerID != null) {
            payer = ledger.getAccount(payerID);

            if (payer == null) {
                throw new ValidateException(Resources.PAYER_ACCOUNT_NOT_FOUND);
            }
        } else {
            payer = ledger.getAccount(senderID);

            if (payer == null) {
                throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
            }
        }

        BalanceProperty balance = AccountProperties.getBalance(payer);
        if (balance.getValue() < fee) {
            throw new ValidateException(Resources.NOT_ENOUGH_FEE);
        }
    }

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

        ensureValidState(ledger);

        // Update payer account
        Account payer;
        if (payerID != null) {
            payer = ledger.getAccount(payerID);
        } else {
            payer = ledger.getAccount(senderID);
        }
        BalanceProperty newSenderBalance = AccountProperties.getBalance(payer).withdraw(fee);
        payer = AccountProperties.setProperty(payer, newSenderBalance);

        return ledger.putAccount(payer);
    }
}
