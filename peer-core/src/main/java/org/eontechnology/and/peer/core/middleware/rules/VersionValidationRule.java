package org.eontechnology.and.peer.core.middleware.rules;

import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.IValidationRule;
import org.eontechnology.and.peer.core.middleware.ValidationResult;

public class VersionValidationRule implements IValidationRule {

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {
        if (tx.getVersion() != 1) {
            return ValidationResult.error("Version is not supported.");
        }
        return ValidationResult.success;
    }
}
