package com.exscudo.peer.core.middleware.rules;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class EmptyPayerValidationRule implements IValidationRule {

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {
        if (tx.getPayer() != null) {
            return ValidationResult.error("Forbidden.");
        }
        if (tx.hasNestedTransactions()) {
            for (Transaction transaction : tx.getNestedTransactions().values()) {
                ValidationResult result = this.validate(transaction, ledger);
                if (result.hasError) {
                    return result;
                }
            }
        }
        return ValidationResult.success;
    }
}
