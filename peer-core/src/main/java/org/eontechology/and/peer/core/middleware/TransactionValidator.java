package org.eontechology.and.peer.core.middleware;

import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.ledger.ILedger;

public class TransactionValidator {

    private final IValidationRule[] rules;

    public TransactionValidator(IValidationRule[] rules) {
        this.rules = rules;
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
