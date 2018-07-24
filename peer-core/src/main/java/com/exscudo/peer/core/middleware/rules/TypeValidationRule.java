package com.exscudo.peer.core.middleware.rules;

import java.util.Set;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class TypeValidationRule implements IValidationRule {
    private final Set<Integer> allowedTypes;

    public TypeValidationRule(Set<Integer> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {

        if (allowedTypes.contains(tx.getType())) {
            return ValidationResult.success;
        }

        return ValidationResult.error("Invalid transaction type. Type :" + tx.getType());
    }
}
