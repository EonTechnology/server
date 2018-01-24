package com.exscudo.peer.eon.transactions.rules;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.state.ColoredBalance;
import com.exscudo.peer.eon.state.ColoredCoin;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import com.exscudo.peer.eon.utils.ColoredCoinId;

public class ColoredCoinPaymentValidationRule implements IValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		final Map<String, Object> data = tx.getData();
		if (data == null || data.size() != 3) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		// validate color
		long color;
		try {
			color = ColoredCoinId.convert(String.valueOf(data.get("color")));
		} catch (IllegalArgumentException e) {
			return ValidationResult.error("The 'color' field value has a unsupported format.");
		}
		IAccount coinAccount = ledger.getAccount(color);
		if (coinAccount == null) {
			return ValidationResult.error("Unknown colored coin.");
		}
		ColoredCoin coloredCoin = AccountProperties.getColoredCoinRegistrationData(coinAccount);
		if (coloredCoin == null) {
			return ValidationResult.error("Account is not associated with a colored coin.");
		}

		// check available funds on the balance of the sender
		long amount;
		try {
			amount = Long.parseLong(String.valueOf(data.get("amount")));
			if (amount <= 0) {
				return ValidationResult.error("The 'amount' field value is out of range.");
			}
		} catch (NumberFormatException e) {
			return ValidationResult.error("The 'amount' field value has a unsupported format.");
		}
		IAccount sender = ledger.getAccount(tx.getSenderID());
		if (sender == null) {
			return ValidationResult.error("Unknown sender.");
		}
		ColoredBalance senderColoredBalance = AccountProperties.getColoredBalance(sender);
		if (amount > senderColoredBalance.getBalance(color)) {
			return ValidationResult.error("Insufficient funds.");
		}

		// check existence of the recipient
		long recipientID;
		try {
			recipientID = Format.ID.accountId(String.valueOf(data.get("recipient")));
		} catch (IllegalArgumentException e) {
			return ValidationResult.error("The 'recipient' field value has a unsupported format.");
		}
		IAccount recipient = ledger.getAccount(recipientID);
		if (recipient == null) {
			return ValidationResult.error("Unknown recipient.");
		}

		return ValidationResult.success;
	}

}
