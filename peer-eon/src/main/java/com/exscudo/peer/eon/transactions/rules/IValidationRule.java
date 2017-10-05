package com.exscudo.peer.eon.transactions.rules;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;

public interface IValidationRule {

	ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context);

}
