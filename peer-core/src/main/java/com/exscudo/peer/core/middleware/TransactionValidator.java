package com.exscudo.peer.core.middleware;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.ITimeProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.rules.ConfirmationsValidationRule;
import com.exscudo.peer.core.middleware.rules.DeadlineValidationRule;
import com.exscudo.peer.core.middleware.rules.ExpiredTimestampValidationRule;
import com.exscudo.peer.core.middleware.rules.FeePerByteValidationRule;
import com.exscudo.peer.core.middleware.rules.FutureTimestampValidationRule;
import com.exscudo.peer.core.middleware.rules.LengthValidationRule;
import com.exscudo.peer.core.middleware.rules.NestedTransactionsValidationRule;
import com.exscudo.peer.core.middleware.rules.NoteValidationRule;
import com.exscudo.peer.core.middleware.rules.ReferencedTransactionValidationRule;
import com.exscudo.peer.core.middleware.rules.SignatureValidationRule;
import com.exscudo.peer.core.middleware.rules.TypeValidationRule;
import com.exscudo.peer.core.middleware.rules.VersionValidationRule;

public class TransactionValidator {

    private final IValidationRule[] rules;

    public TransactionValidator(IValidationRule[] rules) {
        this.rules = rules;
    }

    public static TransactionValidator getAllValidators(IFork fork, ITimeProvider timeProvider) {
        return getAllValidators(fork, timeProvider, timeProvider);
    }

    public static TransactionValidator getAllValidators(IFork fork,
                                                        ITimeProvider blockProvider,
                                                        ITimeProvider peerProvider) {
        return new TransactionValidator(new IValidationRule[] {
                new DeadlineValidationRule(),
                new LengthValidationRule(),
                new ReferencedTransactionValidationRule(),
                new FeePerByteValidationRule(),
                new VersionValidationRule(),
                new NoteValidationRule(fork, blockProvider),
                new TypeValidationRule(fork, blockProvider),
                new ExpiredTimestampValidationRule(blockProvider),
                new FutureTimestampValidationRule(peerProvider),
                new ConfirmationsValidationRule(fork, blockProvider),
                new SignatureValidationRule(fork, blockProvider),
                new NestedTransactionsValidationRule(fork, blockProvider)
        });
    }

    public ValidationResult validate(Transaction transaction, ILedger ledger) {
        for (IValidationRule rule : rules) {
            ValidationResult r = rule.validate(transaction, ledger);
            if (r.hasError) {
                return r;
            }
        }
        return ValidationResult.success;
    }
}
