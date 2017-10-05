package com.exscudo.peer.eon.transactions.rules;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.exceptions.LifecycleException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.transactions.utils.AccountAttributes;
import com.exscudo.peer.eon.transactions.utils.AccountBalance;

public class BaseValidationRule implements IValidationRule {

	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		if (tx.isExpired(context.timestamp) || tx.getDeadline() < 1
				|| tx.getDeadline() > EonConstant.TRANSACTION_MAX_LIFETIME) {
			return ValidationResult.error(new LifecycleException());
		}

		if (tx.getFee() < EonConstant.TRANSACTION_MIN_FEE || tx.getFee() > EonConstant.TRANSACTION_MAX_FEE) {
			return ValidationResult.error("Invalid fee.");
		}

		IAccount sender = ledger.getAccount(tx.getSenderID());
		if (sender == null) {
			return ValidationResult.error("Unknown sender.");
		}

		if (AccountBalance.getBalance(sender) < tx.getFee()) {
			return ValidationResult.error("Not enough funds.");
		}

		if (!tx.verifySignature(AccountAttributes.getPublicKey(sender))) {
			return ValidationResult.error(new IllegalSignatureException());
		}

		return ValidationResult.success;

	}

}
