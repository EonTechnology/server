package org.eontechology.and.peer.core.middleware.rules;

import org.eontechology.and.peer.core.Constant;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.middleware.IValidationRule;
import org.eontechology.and.peer.core.middleware.ValidationResult;

public class LengthValidationRule implements IValidationRule {
    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {
        int length = tx.getLength();
        if (length > Constant.TRANSACTION_MAX_PAYLOAD_LENGTH) {
            return ValidationResult.error("Invalid transaction length.");
        }
        return ValidationResult.success;
    }
}
