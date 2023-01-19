package org.eontechnology.and.peer.core.middleware.rules;

import java.util.regex.Pattern;
import org.eontechnology.and.peer.core.Constant;

public class NoteValidationRuleV2 extends NoteValidationRule {
  private static final String QUOTE =
      Pattern.quote(
          "-_.~abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!*'();:@&=+$,/?%#[] ");

  public NoteValidationRuleV2() {
    super(Pattern.compile("^[" + QUOTE + "]+$"), Constant.TRANSACTION_NOTE_MAX_LENGTH_V2);
  }
}
