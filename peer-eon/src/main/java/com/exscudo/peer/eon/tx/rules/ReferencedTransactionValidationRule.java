package com.exscudo.peer.eon.tx.rules;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;

public class ReferencedTransactionValidationRule implements IValidationRule {

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {
        if (tx.getReference() != null) {
            return ValidationResult.error("Illegal reference.");
        }
        return ValidationResult.success;
    }
}
