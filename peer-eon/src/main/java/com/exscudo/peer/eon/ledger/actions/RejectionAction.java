package com.exscudo.peer.eon.ledger.actions;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.ledger.state.VotePollsProperty;

public class RejectionAction implements ILedgerAction {
    private final AccountID accountID;
    private final AccountID delegateID;

    public RejectionAction(AccountID delegateID, AccountID accountID) {
        this.accountID = accountID;
        this.delegateID = delegateID;
    }

    private ValidationResult canRun(ILedger ledger) {
        Account delegate = ledger.getAccount(delegateID);
        if (delegate == null) {
            return ValidationResult.error("Unknown account.");
        }

        Account account = ledger.getAccount(accountID);
        if (account == null) {
            return ValidationResult.error("Unknown target account.");
        }

        ValidationModeProperty validationMode = AccountProperties.getValidationMode(account);
        if (!validationMode.containWeightForAccount(delegate.getID())) {
            return ValidationResult.error("Account does not participate in transaction confirmation.");
        }
        if (validationMode.getMaxWeight() == validationMode.getWeightForAccount(delegate.getID())) {
            return ValidationResult.error("Rejection is not possible.");
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
        Account sender = newLedger.getAccount(delegateID);
        VotePollsProperty senderVoter = AccountProperties.getVoter(sender);

        senderVoter.setPoll(accountID, 0);
        senderVoter.setTimestamp(context.getTimestamp());
        sender = AccountProperties.setProperty(sender, senderVoter);
        newLedger = newLedger.putAccount(sender);

        // Update target account
        Account target = newLedger.getAccount(accountID);
        ValidationModeProperty targetValidationMode = AccountProperties.getValidationMode(target);
        targetValidationMode.setWeightForAccount(delegateID, 0);
        targetValidationMode.setTimestamp(context.getTimestamp());
        target = AccountProperties.setProperty(target, targetValidationMode);
        newLedger = newLedger.putAccount(target);

        return newLedger;
    }
}
