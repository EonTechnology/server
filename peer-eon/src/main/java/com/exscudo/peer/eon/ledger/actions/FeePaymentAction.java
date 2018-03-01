package com.exscudo.peer.eon.ledger.actions;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;

public class FeePaymentAction implements ILedgerAction {
    private final AccountID senderID;
    private final long fee;

    public FeePaymentAction(AccountID senderID, long fee) {
        this.senderID = senderID;
        this.fee = fee;
    }

    private ValidationResult canRun(ILedger ledger) {
        Account sender = ledger.getAccount(senderID);
        if (sender == null) {
            return ValidationResult.error("Unknown account.");
        }
        BalanceProperty balance = AccountProperties.getBalance(sender);
        if (balance.getValue() < fee) {
            return ValidationResult.error("Not enough funds.");
        }
        return ValidationResult.success;
    }

    @Override
    public ILedger run(ILedger ledger, TransactionContext context) throws ValidateException {
        ValidationResult r = canRun(ledger);
        if (r.hasError) {
            throw r.cause;
        }

        ILedger newLedger = ledger;

        // Update sender account
        Account sender = newLedger.getAccount(senderID);
        BalanceProperty newSenderBalance = AccountProperties.getBalance(sender).withdraw(fee);
        sender = AccountProperties.setProperty(sender, newSenderBalance);
        newLedger = newLedger.putAccount(sender);

        return newLedger;
    }
}
