package org.eontechnology.and.peer.eon.midleware.actions;

import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.LedgerActionContext;
import org.eontechnology.and.peer.eon.ledger.AccountProperties;
import org.eontechnology.and.peer.eon.ledger.state.BalanceProperty;
import org.eontechnology.and.peer.eon.midleware.Resources;

public class PaymentAction implements ILedgerAction {
    private final AccountID recipientID;
    private final AccountID senderID;
    private final long amount;

    public PaymentAction(AccountID senderID, long amount, AccountID recipientID) {
        this.recipientID = recipientID;
        this.senderID = senderID;
        this.amount = amount;
    }

    private void ensureValidState(ILedger ledger) throws ValidateException {
        Account sender = ledger.getAccount(senderID);
        if (sender == null) {
            throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
        }

        Account recipient = ledger.getAccount(recipientID);
        if (recipient == null) {
            throw new ValidateException(Resources.RECIPIENT_ACCOUNT_NOT_FOUND);
        }

        BalanceProperty balance = AccountProperties.getBalance(sender);
        if (balance.getValue() < amount) {
            throw new ValidateException(Resources.NOT_ENOUGH_FUNDS);
        }
    }

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

        ensureValidState(ledger);

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
