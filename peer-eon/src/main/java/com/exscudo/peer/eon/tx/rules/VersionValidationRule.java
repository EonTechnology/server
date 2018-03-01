package com.exscudo.peer.eon.tx.rules;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;

public class VersionValidationRule implements IValidationRule {

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {
        if (tx.getVersion() != 1) {
            return ValidationResult.error("Version is not supported.");
        }
        return ValidationResult.success;
    }
}
