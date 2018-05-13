package com.exscudo.peer.core.middleware.rules;

import java.util.Set;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.ITimeProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class TypeValidationRule implements IValidationRule {
    private final IFork fork;
    private final ITimeProvider timeProvider;

    public TypeValidationRule(IFork fork, ITimeProvider timeProvider) {
        this.fork = fork;
        this.timeProvider = timeProvider;
    }

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {

        Set<Integer> allowedTypes = fork.getTransactionTypes(timeProvider.get());
        if (allowedTypes.contains(tx.getType())) {
            return ValidationResult.success;
        }

        return ValidationResult.error("Invalid transaction type. Type :" + tx.getType());
    }
}
