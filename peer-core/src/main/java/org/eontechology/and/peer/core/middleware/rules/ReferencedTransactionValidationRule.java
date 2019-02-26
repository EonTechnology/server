package org.eontechology.and.peer.core.middleware.rules;

import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.middleware.IValidationRule;
import org.eontechology.and.peer.core.middleware.ValidationResult;

public class ReferencedTransactionValidationRule implements IValidationRule {

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {
        if (tx.getReference() != null) {
            return ValidationResult.error("Illegal reference.");
        }
        return ValidationResult.success;
    }
}
