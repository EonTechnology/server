package com.exscudo.peer.core.middleware.rules;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

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
