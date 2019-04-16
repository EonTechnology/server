package org.eontechnology.and.peer.core.middleware.rules;

import java.util.Map;
import java.util.Set;

import org.eontechnology.and.peer.core.common.IAccountHelper;
import org.eontechnology.and.peer.core.common.ITimeProvider;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.TransactionID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.IValidationRule;
import org.eontechnology.and.peer.core.middleware.TransactionValidator;
import org.eontechnology.and.peer.core.middleware.ValidationResult;

public class NestedTransactionsValidationRule implements IValidationRule {
    private final TransactionValidator transactionValidator;

    public NestedTransactionsValidationRule(Set<Integer> allowedTypes,
                                            ITimeProvider timeProvider,
                                            IAccountHelper accountHelper) {

        this.transactionValidator = new TransactionValidator(new IValidationRule[] {
                new NestedTransaction_FeeValidationRule(),
                new NestedTransaction_NestedTransactionValidationRule(),
                new LengthValidationRule(),
                new DeadlineValidationRule(),
                new VersionValidationRule(),
                new NoteValidationRule(),
                new TypeValidationRule(allowedTypes),
                new ExpiredTimestampValidationRule(timeProvider),
                new ConfirmationsSetValidationRule(timeProvider, accountHelper) {{
                    setAllowPayer(false);
                }},
                new ConfirmationsValidationRule(timeProvider, accountHelper),
                new SignatureValidationRule(timeProvider, accountHelper)
        });
    }

    @Override
    public ValidationResult validate(Transaction transaction, ILedger ledger) {

        if (transaction.getNestedTransactions() == null) {
            return ValidationResult.success;
        }

        if (transaction.getNestedTransactions().size() == 0) {
            return ValidationResult.error("Illegal usage.");
        }

        for (Map.Entry<String, Transaction> entry : transaction.getNestedTransactions().entrySet()) {

            TransactionID id = new TransactionID(entry.getKey());
            Transaction nestedTx = entry.getValue();

            if (!id.equals(nestedTx.getID())) {
                return ValidationResult.error("ID does not match transaction.");
            }

            if (nestedTx.getTimestamp() > transaction.getTimestamp()) {
                return ValidationResult.error("Invalid nested transaction. Invalid timestamp.");
            }

            AccountID payerID = nestedTx.getPayer();
            if (payerID != null && !payerID.equals(transaction.getSenderID())) {
                return ValidationResult.error("Invalid nested transaction. Invalid payer.");
            }

            if (nestedTx.getReference() != null &&
                    !transaction.getNestedTransactions().containsKey(nestedTx.getReference().toString())) {
                return ValidationResult.error("Invalid nested transaction. Invalid reference.");
            }

            ValidationResult r = transactionValidator.validate(nestedTx, ledger);
            if (r.hasError) {
                return ValidationResult.error("Invalid nested transaction. " + r.cause.getMessage());
            }
        }

        return ValidationResult.success;
    }

    private static class NestedTransaction_FeeValidationRule implements IValidationRule {
        @Override
        public ValidationResult validate(Transaction tx, ILedger ledger) {
            if (tx.getFee() != 0L) {
                return ValidationResult.error("A fee must be equal a zero.");
            }
            return ValidationResult.success;
        }
    }

    private static class NestedTransaction_NestedTransactionValidationRule implements IValidationRule {
        @Override
        public ValidationResult validate(Transaction tx, ILedger ledger) {
            if (tx.getNestedTransactions() != null) {
                return ValidationResult.error("Nested transactions are allowed only at the top level.");
            }
            return ValidationResult.success;
        }
    }
}
