package com.exscudo.peer.core.middleware.rules;

import java.util.Map;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.ITimeProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.TransactionValidator;
import com.exscudo.peer.core.middleware.ValidationResult;

public class NestedTransactionsValidationRule implements IValidationRule {
    private final IFork fork;
    private final ITimeProvider timeProvider;
    private final TransactionValidator transactionValidator;

    public NestedTransactionsValidationRule(IFork fork, ITimeProvider timeProvider) {
        this.fork = fork;
        this.timeProvider = timeProvider;

        this.transactionValidator = new TransactionValidator(new IValidationRule[] {
                new NestedTransaction_FeeValidationRule(),
                new NestedTransaction_NestedTransactionValidationRule(),
                new LengthValidationRule(),
                new DeadlineValidationRule(),
                new VersionValidationRule(),
                new NoteValidationRule(fork, timeProvider),
                new TypeValidationRule(fork, timeProvider),
                new ExpiredTimestampValidationRule(timeProvider),
                new ConfirmationsValidationRule(fork, timeProvider),
                new SignatureValidationRule(fork, timeProvider)
        });
    }

    @Override
    public ValidationResult validate(Transaction transaction, ILedger ledger) {

        if (transaction.getNestedTransactions() == null) {
            return ValidationResult.success;
        }

        if (transaction.getNestedTransactions().size() < 2) {
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
