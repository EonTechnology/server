package com.exscudo.peer.core.middleware.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.common.ITimeProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.IValidationRule;
import com.exscudo.peer.core.middleware.ValidationResult;

public class NoteValidationRule implements IValidationRule {
    private final IFork fork;
    private final ITimeProvider timeProvider;

    private final Pattern notePattern = Pattern.compile("^[-a-zA-Z0-9 #@*_]+$");

    public NoteValidationRule(IFork fork, ITimeProvider timeProvider) {
        this.fork = fork;
        this.timeProvider = timeProvider;
    }

    @Override
    public ValidationResult validate(Transaction tx, ILedger ledger) {

        if (tx.getNote() == null) {
            return ValidationResult.success;
        }

        int maxLength = fork.getMaxNoteLength(timeProvider.get());

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
