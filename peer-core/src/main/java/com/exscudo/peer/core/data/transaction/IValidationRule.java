package com.exscudo.peer.core.data.transaction;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.ledger.ILedger;

public interface IValidationRule {

    ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context);
}
