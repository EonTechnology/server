package com.exscudo.peer.core.middleware.rules;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.ITimeProvider;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class ConfirmationsValidationRule implements IValidationRule {
    private final IFork fork;
    private final ITimeProvider timeProvider;

    public ConfirmationsValidationRule(IFork fork, ITimeProvider timeProvider) {
        this.fork = fork;
        this.timeProvider = timeProvider;
    }

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {

        Account sender = ledger.getAccount(tx.getSenderID());
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }

        Set<AccountID> accounts = fork.getConfirmingAccounts(sender, timeProvider.get());
        if (accounts != null) {

            // check signatures
            Map<AccountID, Account> set = new HashMap<>();
            set.put(tx.getSenderID(), sender);
            if (tx.getConfirmations() != null) {

                if (tx.getConfirmations().size() > Constant.TRANSACTION_CONFIRMATIONS_MAX_SIZE) {
                    return ValidationResult.error("Invalid use of the confirmation field.");
                }

                for (Map.Entry<String, Object> entry : tx.getConfirmations().entrySet()) {

                    AccountID id;
                    try {
                        id = new AccountID(entry.getKey());
                    } catch (Exception e) {
                        return ValidationResult.error("Invalid format.");
                    }

                    if (!accounts.contains(id)) {
                        return ValidationResult.error("Account '" + id.toString() + "' can not sign transaction.");
                    }

                    Account account = ledger.getAccount(id);
                    if (account == null) {
                        return ValidationResult.error("Unknown account " + id.toString());
                    }

                    set.put(id, account);
                }
            }

            if (!fork.validConfirmation(tx, set, timeProvider.get())) {
                return ValidationResult.error("The quorum is not exist.");
            }
        } else {
            if (tx.getConfirmations() != null) {
                return ValidationResult.error("Invalid use of the confirmation field.");
            }
        }

        return ValidationResult.success;
    }
}
