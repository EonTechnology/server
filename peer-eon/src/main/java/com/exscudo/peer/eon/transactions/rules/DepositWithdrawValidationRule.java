package com.exscudo.peer.eon.transactions.rules;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.state.GeneratingBalance;
import com.exscudo.peer.eon.transactions.Deposit;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class DepositWithdrawValidationRule implements IValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		if (tx.getFee() != Deposit.DEPOSIT_TRANSACTION_FEE) {
			return ValidationResult.error("The field value Fee is not valid.");
		}

		final Map<String, Object> data = tx.getData();
		if (data == null || data.size() != 1) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		long amount;
		try {
			amount = Long.parseLong(String.valueOf(data.get("amount")));
		} catch (NumberFormatException e) {
			return ValidationResult.error("Attachment of unknown type.");
		}
		if (amount <= 0) {
			return ValidationResult.error("Invalid amount.");
		}

		IAccount account = ledger.getAccount(tx.getSenderID());
		if (account == null) {
			return ValidationResult.error("Unknown sender.");
		}

		Balance balance = AccountProperties.getBalance(account);
		if (balance == null || balance.getValue() < tx.getFee()) {
			return ValidationResult.error("Not enough funds.");
		}

		GeneratingBalance deposit = AccountProperties.getDeposit(account);
		if (deposit == null || deposit.getValue() < amount) {
			return ValidationResult.error("Not enough funds on deposit.");
		}

		return ValidationResult.success;
	}

}
