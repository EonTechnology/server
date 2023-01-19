package org.eontechnology.and.peer.core.middleware.rules;

import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.IValidationRule;
import org.eontechnology.and.peer.core.middleware.ValidationResult;

public class PayerValidationRule implements IValidationRule {

  @Override
  public ValidationResult validate(Transaction tx, ILedger ledger) {

    if (tx.getPayer() == null) {
      return ValidationResult.success;
    }

    if (tx.getSenderID().equals(tx.getPayer())) {
      return ValidationResult.error("Invalid payer");
    }

    if (tx.getConfirmations() == null
        || !tx.getConfirmations().containsKey(tx.getPayer().toString())) {
      return ValidationResult.error("Payer not confirm");
    }

    return ValidationResult.success;
  }
}
