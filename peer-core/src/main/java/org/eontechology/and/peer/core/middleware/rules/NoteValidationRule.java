package org.eontechology.and.peer.core.middleware.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eontechology.and.peer.core.Constant;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.middleware.IValidationRule;
import org.eontechology.and.peer.core.middleware.ValidationResult;

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
