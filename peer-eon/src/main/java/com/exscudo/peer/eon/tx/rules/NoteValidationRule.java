package com.exscudo.peer.eon.tx.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.EonConstant;

public class NoteValidationRule implements IValidationRule {
    private final int maxLength;
    private final Pattern notePattern = Pattern.compile("^[-a-zA-Z\\d #@*_]+$");

    public NoteValidationRule(int maxLength) {
        this.maxLength = maxLength;
    }

    public NoteValidationRule() {
        this(EonConstant.TRANSACTION_NOTE_MAX_LENGTH);
    }

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

        if (tx.getNote() == null) {
            return ValidationResult.success;
        }

        int length = tx.getNote().length();
        if (length > 0 && length <= maxLength) {

            Matcher m = notePattern.matcher(tx.getNote());
            if (m.matches()) {
                return ValidationResult.success;
            }
        }

        return ValidationResult.error("Invalid note.");
    }
}
