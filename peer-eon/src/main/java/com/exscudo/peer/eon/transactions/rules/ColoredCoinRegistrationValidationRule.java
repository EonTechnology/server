package com.exscudo.peer.eon.transactions.rules;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.state.ColoredCoin;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class ColoredCoinRegistrationValidationRule implements IValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		IAccount sender = ledger.getAccount(tx.getSenderID());
		if (sender == null) {
			return ValidationResult.error("Unknown sender.");
		}
		if (AccountProperties.getColoredCoinRegistrationData(sender) != null) {
			return ValidationResult.error("Account is already associated with a color coin.");
		}

		final Map<String, Object> data = tx.getData();
		if (data == null || data.size() != 2) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		long moneySupply;
		try {
			moneySupply = Long.parseLong(String.valueOf(data.get("emission")));
		} catch (NumberFormatException e) {
			return ValidationResult.error("The 'emission' field value has a unsupported format.");
		}
		if (moneySupply < ColoredCoin.MIN_EMISSION_SIZE) {
			return ValidationResult.error("The 'emission' field value out of range.");
		}

		int decimalPoint;
		try {
			decimalPoint = Integer.parseInt(String.valueOf(data.get("decimalPoint")));
		} catch (NumberFormatException e) {
			return ValidationResult.error("The 'decimalPoint' field value has a unsupported format.");
		}
		if (decimalPoint < ColoredCoin.MIN_DECIMAL_POINT || decimalPoint > ColoredCoin.MAX_DECIMAL_POINT) {
			return ValidationResult.error("The 'decimalPoint' field value is out of range.");
		}

		return ValidationResult.success;

	}

}
