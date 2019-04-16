package org.eontechnology.and.peer.core.middleware;

import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.ledger.ILedger;

/**
 * The basic interface for an object that validate the
 * underlying transaction fields.
 */
public interface IValidationRule {

    ValidationResult validate(Transaction tx, ILedger ledger);
}
