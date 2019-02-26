package org.eontechology.and.peer.core.middleware.rules;

import org.eontechology.and.peer.core.common.ITimeProvider;
import org.eontechology.and.peer.core.common.ImmutableTimeProvider;
import org.eontechology.and.peer.core.common.exceptions.LifecycleException;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.middleware.IValidationRule;
import org.eontechology.and.peer.core.middleware.ValidationResult;

public class ExpiredTimestampValidationRule implements IValidationRule {
    private final ITimeProvider timeProvider;

    public ExpiredTimestampValidationRule(ITimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public ExpiredTimestampValidationRule(int timestamp) {
        this(new ImmutableTimeProvider(timestamp));
    }

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {
        if (tx.isExpired(timeProvider.get())) {
            return ValidationResult.error(new LifecycleException());
        }

        return ValidationResult.success;
    }
}
