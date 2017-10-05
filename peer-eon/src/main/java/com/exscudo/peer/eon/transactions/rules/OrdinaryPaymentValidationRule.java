package com.exscudo.peer.eon.transactions.rules;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.transactions.Payment;
import com.exscudo.peer.eon.transactions.utils.AccountBalance;

public class OrdinaryPaymentValidationRule extends BaseValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		ValidationResult r = super.validate(tx, ledger, context);
		if (r.hasError) {
			return r;
		}

		IAccount sender = ledger.getAccount(tx.getSenderID());
		if (sender == null) {
			return ValidationResult.error("Unknown sender.");
		}

		final Map<String, Object> data = tx.getData();
		if (data == null || data.size() != 2 || !data.containsKey("amount") || !data.containsKey("recipient")) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		final Object amountObj = data.get("amount");
		if (!(amountObj instanceof Long) && !(amountObj instanceof Integer)) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		final Object recipientObj = data.get("recipient");
		if (!(recipientObj instanceof String)) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		long recipientID;
		try {
			recipientID = Format.ID.accountId(recipientObj.toString());
		} catch (IllegalArgumentException e) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		IAccount recipient = ledger.getAccount(recipientID);
		if (recipient == null) {
			return ValidationResult.error("Unknown recipient.");
		}

		long amount = Long.parseLong(amountObj.toString());
		if (amount < Payment.MIN_PAYMENT || amount > Payment.MAX_PAYMENT) {
			return ValidationResult.error("Invalid fee or amount.");
		}

		if (AccountBalance.getBalance(sender) < tx.getFee() + amount) {
			return ValidationResult.error("Not enough funds.");
		}

		return ValidationResult.success;
	}

}
