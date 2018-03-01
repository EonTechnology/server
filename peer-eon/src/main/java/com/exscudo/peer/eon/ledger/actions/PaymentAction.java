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

public class PaymentAction implements ILedgerAction {
    private final AccountID recipientID;
    private final AccountID senderID;
    private final long amount;

    public PaymentAction(AccountID senderID, long amount, AccountID recipientID) {
        this.recipientID = recipientID;
        this.senderID = senderID;
        this.amount = amount;
    }

    private ValidationResult canRun(ILedger ledger) {
        Account sender = ledger.getAccount(senderID);
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }

        Account recipient = ledger.getAccount(recipientID);
        if (recipient == null) {
            return ValidationResult.error("Unknown recipient.");
        }

        BalanceProperty balance = AccountProperties.getBalance(sender);
        if (balance.getValue() < amount) {
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
        BalanceProperty newSenderBalance = AccountProperties.getBalance(sender).withdraw(amount);
        sender = AccountProperties.setProperty(sender, newSenderBalance);
        newLedger = newLedger.putAccount(sender);

        // Update recipient account
        Account recipient = newLedger.getAccount(recipientID);
        BalanceProperty newRecipientBalance = AccountProperties.getBalance(recipient).refill(amount);
        recipient = AccountProperties.setProperty(recipient, newRecipientBalance);
        newLedger = newLedger.putAccount(recipient);

        return newLedger;
    }
}
