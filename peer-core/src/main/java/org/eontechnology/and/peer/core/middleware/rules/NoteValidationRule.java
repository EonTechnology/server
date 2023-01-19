package org.eontechnology.and.peer.core.middleware.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.IValidationRule;
import org.eontechnology.and.peer.core.middleware.ValidationResult;

public class NoteValidationRule implements IValidationRule {

  private final Pattern notePattern;
  private final int maxLength;

  public NoteValidationRule() {
    this(Pattern.compile("^[-a-zA-Z0-9 #@*_]+$"), Constant.TRANSACTION_NOTE_MAX_LENGTH);
  }

  public NoteValidationRule(Pattern notePattern, int maxLength) {
    this.notePattern = notePattern;
    this.maxLength = maxLength;
  }

  @Override
  public ValidationResult validate(Transaction tx, ILedger ledger) {

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

    return ValidationResult.error("Invalid note");
  }
}
