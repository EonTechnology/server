package com.exscudo.peer.core.middleware.rules;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class ReferencedTransactionValidationRule implements IValidationRule {

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {
        if (tx.getReference() != null) {
            return ValidationResult.error("Illegal reference.");
        }
        return ValidationResult.success;
    }
}
