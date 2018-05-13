package com.exscudo.peer.core.middleware.rules;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class VersionValidationRule implements IValidationRule {

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {
        if (tx.getVersion() != 1) {
            return ValidationResult.error("Version is not supported.");
        }
        return ValidationResult.success;
    }
}
