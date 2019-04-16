package org.eontechnology.and.peer.core.middleware.rules;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.common.IAccountHelper;
import org.eontechnology.and.peer.core.common.ITimeProvider;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.IValidationRule;
import org.eontechnology.and.peer.core.middleware.ValidationResult;

public class ConfirmationsValidationRule implements IValidationRule {
    private final ITimeProvider timeProvider;
    private final IAccountHelper accountHelper;

    public ConfirmationsValidationRule(ITimeProvider timeProvider, IAccountHelper accountHelper) {
        this.timeProvider = timeProvider;
        this.accountHelper = accountHelper;
    }

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {

        Account sender = ledger.getAccount(tx.getSenderID());
        if (sender == null) {
            return ValidationResult.error("Unknown sender.");
        }

        Set<AccountID> accounts = accountHelper.getConfirmingAccounts(sender, timeProvider.get());

        if (accounts != null || tx.getConfirmations() != null) {

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

                    Account account = ledger.getAccount(id);
                    if (account == null) {
                        return ValidationResult.error("Unknown account " + id.toString());
                    }

                    set.put(id, account);
                }
            }

            try {
                if (!accountHelper.validConfirmation(tx, set, timeProvider.get())) {
                    return ValidationResult.error("The quorum is not exist.");
                }
            } catch (ValidateException e) {
                return ValidationResult.error(e);
            }
        }

        return ValidationResult.success;
    }
}
