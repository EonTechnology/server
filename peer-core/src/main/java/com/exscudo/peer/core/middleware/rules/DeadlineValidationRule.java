package com.exscudo.peer.core.middleware.rules;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.exceptions.LifecycleException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class DeadlineValidationRule implements IValidationRule {
    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {
        if (tx.getDeadline() < 1 || tx.getDeadline() > Constant.TRANSACTION_MAX_LIFETIME) {
            return ValidationResult.error(new LifecycleException());
        }
        return ValidationResult.success;
    }
}
