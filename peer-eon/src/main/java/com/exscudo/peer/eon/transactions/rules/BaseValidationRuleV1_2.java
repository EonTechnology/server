package com.exscudo.peer.eon.transactions.rules;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.LifecycleException;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.EonConstant;

public class BaseValidationRuleV1_2 implements IValidationRule {
	// deadline * DEADLINE_TIME_UNIT - deadline in seconds
	private static final int DEADLINE_TIME_UNIT = 60;

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		if (tx.getVersion() != 1 && tx.getVersion() != 2) {
			return ValidationResult.error("Version is not supported.");
		}

		if (tx.getLength() > EonConstant.TRANSACTION_MAX_PAYLOAD_LENGTH) {
			return ValidationResult.error("Invalid transaction length.");
		}

		if (tx.getFee() < EonConstant.TRANSACTION_MIN_FEE || tx.getFee() > EonConstant.TRANSACTION_MAX_FEE) {
			return ValidationResult.error("Invalid fee.");
		}

		int timestamp = context.timestamp;
		boolean isExpired = true;
		if (tx.getVersion() == 1) {
			isExpired = (tx.getTimestamp() + tx.getDeadline() * DEADLINE_TIME_UNIT <= timestamp);
		} else if (tx.getVersion() == 2) {
			isExpired = tx.isExpired(timestamp);
		}

		if (isExpired || tx.getDeadline() < 1
				|| !tx.isExpired(tx.getTimestamp() + EonConstant.TRANSACTION_MAX_LIFETIME)) {
			return ValidationResult.error(new LifecycleException());
		}

		return ValidationResult.success;
	}

}
