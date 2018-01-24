package com.exscudo.peer.eon.transactions.rules;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;

public class ReferencedTransactionValidationRule implements IValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {
		if (tx.getReference() != 0) {
			return ValidationResult.error("Illegal reference.");
		}
		return ValidationResult.success;
	}

}
