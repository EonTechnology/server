package com.exscudo.peer.eon.tx.rules;

import com.exscudo.peer.core.common.exceptions.LifecycleException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.EonConstant;

public class BaseValidationRule implements IValidationRule {

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

        int length = tx.getLength();
        if (length > EonConstant.TRANSACTION_MAX_PAYLOAD_LENGTH) {
            return ValidationResult.error("Invalid transaction length.");
        }

        long fee = tx.getFee();
        if (fee < length * EonConstant.TRANSACTION_MIN_FEE_PER_BYTE) {
            return ValidationResult.error("Invalid fee.");
        }

        if (tx.getDeadline() < 1 || tx.getDeadline() > EonConstant.TRANSACTION_MAX_LIFETIME) {
            return ValidationResult.error(new LifecycleException());
        }

        return ValidationResult.success;
    }
}
