package org.eontechnology.and.peer.core.middleware.rules;

import org.eontechnology.and.peer.core.common.ITimeProvider;
import org.eontechnology.and.peer.core.common.ImmutableTimeProvider;
import org.eontechnology.and.peer.core.common.exceptions.LifecycleException;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.IValidationRule;
import org.eontechnology.and.peer.core.middleware.ValidationResult;

public class FutureTimestampValidationRule implements IValidationRule {
  private final ITimeProvider timeProvider;

  public FutureTimestampValidationRule(ITimeProvider timeProvider) {
    this.timeProvider = timeProvider;
  }

  public FutureTimestampValidationRule(int timestamp) {
    this(new ImmutableTimeProvider(timestamp));
  }

  @Override
  public ValidationResult validate(Transaction tx, ILedger ledger) {

    if (tx.isFuture(timeProvider.get())) {
      return ValidationResult.error(new LifecycleException());
    }

    return ValidationResult.success;
  }
}
