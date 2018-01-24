package com.exscudo.peer.eon.transactions.rules;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.transactions.builders.PaymentBuilder;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class OrdinaryPaymentValidationRule implements IValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		IAccount sender = ledger.getAccount(tx.getSenderID());
		if (sender == null) {
			return ValidationResult.error("Unknown sender.");
		}

		final Map<String, Object> data = tx.getData();
		if (data == null || data.size() != 2 || !data.containsKey("amount") || !data.containsKey("recipient")) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		long amount = 0;
		try {
			amount = Long.parseLong(String.valueOf(data.get("amount")));
		} catch (NumberFormatException e) {
			return ValidationResult.error("Attachment of unknown type. The amount format is not supports.");
		}
		if (amount < PaymentBuilder.MIN_PAYMENT || amount > PaymentBuilder.MAX_PAYMENT) {
			return ValidationResult.error("Invalid amount size.");
		}

		long recipientID;
		try {
			recipientID = Format.ID.accountId(String.valueOf(data.get("recipient")));
		} catch (IllegalArgumentException e) {
			return ValidationResult.error("Attachment of unknown type. The recipient format is not supports.");
		}

		IAccount recipient = ledger.getAccount(recipientID);
		if (recipient == null) {
			return ValidationResult.error("Unknown recipient.");
		}

		Balance balance = AccountProperties.getBalance(sender);
		if (balance == null || balance.getValue() < tx.getFee() + amount) {
			return ValidationResult.error("Not enough funds.");
		}

		return ValidationResult.success;
	}

}
