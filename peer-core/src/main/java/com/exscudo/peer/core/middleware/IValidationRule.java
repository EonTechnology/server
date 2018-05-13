package com.exscudo.peer.core.middleware;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;

/**
 * The basic interface for an object that validate the
 * underlying transaction fields.
 */
public interface IValidationRule {

    ValidationResult validate(Transaction tx, ILedger ledger);
}
