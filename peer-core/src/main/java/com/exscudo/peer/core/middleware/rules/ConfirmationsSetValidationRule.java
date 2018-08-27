package com.exscudo.peer.core.middleware.rules;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.exscudo.peer.core.common.IAccountHelper;
import com.exscudo.peer.core.common.ITimeProvider;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class ConfirmationsSetValidationRule implements IValidationRule {
    private final ITimeProvider timeProvider;
    private final IAccountHelper accountHelper;
    private boolean allowPayer = true;

    public ConfirmationsSetValidationRule(ITimeProvider timeProvider, IAccountHelper accountHelper) {
        this.timeProvider = timeProvider;
        this.accountHelper = accountHelper;
    }

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {

        Map<String, Object> confirmations = tx.getConfirmations();
        if (confirmations == null) {
            return ValidationResult.success;
        }

        Account sender = ledger.getAccount(tx.getSenderID());
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }

        Set<AccountID> accounts = accountHelper.getConfirmingAccounts(sender, timeProvider.get());

        if (accounts == null) {
            accounts = new HashSet<>();
        } else {
            accounts = new HashSet<>(accounts);
        }

        if (allowPayer && tx.getPayer() != null) {
            accounts.add(tx.getPayer());
        }

        for (String id : confirmations.keySet()) {
            AccountID acc;
            try {
                acc = new AccountID(id);
            } catch (Exception e) {
                return ValidationResult.error("Invalid format.");
            }

            if (!accounts.contains(acc)) {
                return ValidationResult.error("Account '" + acc.toString() + "' can not sign transaction.");
            }
        }

        return ValidationResult.success;
    }

    public boolean isAllowPayer() {
        return allowPayer;
    }

    public void setAllowPayer(boolean allowPayer) {
        this.allowPayer = allowPayer;
    }
}
