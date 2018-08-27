package com.exscudo.peer.core.middleware.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class NoteValidationRule implements IValidationRule {

    private final Pattern notePattern = Pattern.compile("^[-a-zA-Z0-9 #@*_]+$");

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {

        if (tx.getNote() == null) {
            return ValidationResult.success;
        }

        int maxLength = Constant.TRANSACTION_NOTE_MAX_LENGTH;

        int length = tx.getNote().length();
        if (length > 0 && length <= maxLength) {

            Matcher m = notePattern.matcher(tx.getNote());
            if (m.matches()) {
                return ValidationResult.success;
            }
        }

        return ValidationResult.error("Invalid note");
    }
}
