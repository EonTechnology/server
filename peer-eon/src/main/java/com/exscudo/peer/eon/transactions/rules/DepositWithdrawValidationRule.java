package com.exscudo.peer.eon.transactions.rules;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.transactions.Deposit;
import com.exscudo.peer.eon.transactions.utils.AccountDeposit;

public class DepositWithdrawValidationRule extends BaseValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		ValidationResult r = super.validate(tx, ledger, context);
		if (r.hasError) {
			return r;
		}

		if (tx.getFee() != Deposit.DEPOSIT_TRANSACTION_FEE) {
			return ValidationResult.error("The field value Fee is not valid.");
		}

		final Map<String, Object> data = tx.getData();
		if (data == null || data.size() != 1 || !data.containsKey("amount")) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		final Object amountObj = data.get("amount");
		if (!(amountObj instanceof Long) && !(amountObj instanceof Integer)) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		final Long trAmount = Long.parseLong(amountObj.toString());
		if (trAmount <= 0) {
			return ValidationResult.error("Invalid tr amount.");
		}

		IAccount account = ledger.getAccount(tx.getSenderID());
		if (account == null) {
			return ValidationResult.error("Unknown sender.");
		}

		if (AccountDeposit.getDeposit(account) < trAmount) {
			return ValidationResult.error("Not enough funds on deposit.");
		}

		return ValidationResult.success;
	}

}
