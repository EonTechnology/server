package com.exscudo.peer.core.middleware.rules;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class FeePerByteValidationRule implements IValidationRule {

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {

        int length = tx.getLength();
        long fee = tx.getFee();
        if (fee < length * Constant.TRANSACTION_MIN_FEE_PER_BYTE) {
            return ValidationResult.error("Invalid fee.");
        }

        return ValidationResult.success;
    }
}
