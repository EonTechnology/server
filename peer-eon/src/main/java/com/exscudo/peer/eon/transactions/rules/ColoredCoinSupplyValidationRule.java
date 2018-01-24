package com.exscudo.peer.eon.transactions.rules;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.state.ColoredBalance;
import com.exscudo.peer.eon.state.ColoredCoin;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class ColoredCoinSupplyValidationRule implements IValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		final Map<String, Object> data = tx.getData();
		if (data == null || data.size() != 1) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		long newMoneySupply;
		try {
			newMoneySupply = Long.parseLong(String.valueOf(data.get("moneySupply")));
		} catch (NumberFormatException e) {
			return ValidationResult.error("The 'moneySupply' field value has a unsupported format.");
		}
		if (newMoneySupply < 0) {
			return ValidationResult.error("The 'moneySupply' field value is out of range.");
		}

		// validate balance
		IAccount sender = ledger.getAccount(tx.getSenderID());
		if (sender == null) {
			return ValidationResult.error("Unknown sender.");
		}
		ColoredCoin senderColoredCoin = AccountProperties.getColoredCoinRegistrationData(sender);
		if (senderColoredCoin == null) {
			return ValidationResult.error("Colored coin is not associated with an account.");
		}
		if (senderColoredCoin.getMoneySupply() == newMoneySupply) {
			return ValidationResult.error("Value already set.");
		}
		ColoredBalance senderColoredBalance = AccountProperties.getColoredBalance(sender);
		long balance = 0;
		if (senderColoredBalance != null) {
			balance = senderColoredBalance.getBalance(tx.getSenderID());
		}
		if (newMoneySupply == 0) { // detaching colored coin from account
			if (balance != senderColoredCoin.getMoneySupply()) {
				return ValidationResult.error("The entire amount of funds must be on the balance.");
			}
		} else {
			long deltaMoneySupply = newMoneySupply - senderColoredCoin.getMoneySupply();
			if (balance + deltaMoneySupply < 0) {
				return ValidationResult.error("Insufficient number of colored coins on the balance.");
			}
		}

		return ValidationResult.success;

	}

}
