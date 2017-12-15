package com.exscudo.peer.eon.transactions.rules;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class SenderValidationRule implements IValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		IAccount sender = ledger.getAccount(tx.getSenderID());
		if (sender == null) {
			return ValidationResult.error("Unknown sender.");
		}

		Balance balance = AccountProperties.getBalance(sender);
		if (balance == null || balance.getValue() < tx.getFee()) {
			return ValidationResult.error("Not enough funds.");
		}

		if (!tx.verifySignature(AccountProperties.getPublicKey(sender))) {
			return ValidationResult.error(new IllegalSignatureException());
		}

		return ValidationResult.success;

	}

}
