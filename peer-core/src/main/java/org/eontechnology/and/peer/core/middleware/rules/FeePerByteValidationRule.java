package org.eontechnology.and.peer.core.middleware.rules;

import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.IValidationRule;
import org.eontechnology.and.peer.core.middleware.ValidationResult;

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
