package org.eontechology.and.peer.core.middleware.rules;

import org.eontechology.and.peer.core.Constant;
import org.eontechology.and.peer.core.common.exceptions.LifecycleException;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.middleware.IValidationRule;
import org.eontechology.and.peer.core.middleware.ValidationResult;

public class DeadlineValidationRule implements IValidationRule {
    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {
        if (tx.getDeadline() < 1 || tx.getDeadline() > Constant.TRANSACTION_MAX_LIFETIME) {
            return ValidationResult.error(new LifecycleException());
        }
        return ValidationResult.success;
    }
}
