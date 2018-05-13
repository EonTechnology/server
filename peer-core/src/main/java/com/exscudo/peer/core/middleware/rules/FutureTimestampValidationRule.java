package com.exscudo.peer.core.middleware.rules;

import com.exscudo.peer.core.common.ITimeProvider;
import com.exscudo.peer.core.common.ImmutableTimeProvider;
import com.exscudo.peer.core.common.exceptions.LifecycleException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class FutureTimestampValidationRule implements IValidationRule {
    private final ITimeProvider timeProvider;

    public FutureTimestampValidationRule(ITimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public FutureTimestampValidationRule(int timestamp) {
        this(new ImmutableTimeProvider(timestamp));
    }

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {

        if (tx.isFuture(timeProvider.get())) {
            return ValidationResult.error(new LifecycleException());
        }

        return ValidationResult.success;
    }
}