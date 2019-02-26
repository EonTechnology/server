package org.eontechology.and.peer.core.middleware.rules;

import java.util.Set;

import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.middleware.IValidationRule;
import org.eontechology.and.peer.core.middleware.ValidationResult;

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
