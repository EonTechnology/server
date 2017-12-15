package com.exscudo.peer.eon.transactions.rules;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class RejectionValidationRule implements IValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		IAccount sender = ledger.getAccount(tx.getSenderID());
		if (sender == null) {
			return ValidationResult.error("Unknown sender.");
		}

		final Map<String, Object> data = tx.getData();
		if (data == null || data.size() != 1) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		try {

			long id = Format.ID.accountId(String.valueOf(data.get("account")));
			if (id == sender.getID()) {
				return ValidationResult.error("Illegal account.");
			}
			IAccount account = ledger.getAccount(id);
			if (account == null) {
				return ValidationResult.error("Unknown account.");
			}

			ValidationMode validationMode = AccountProperties.getValidationMode(account);
			if (validationMode == null) {
				return ValidationResult.error("The delegates list is not specified.");
			}
			if (!validationMode.containWeightForAccount(tx.getSenderID())) {
				return ValidationResult.error("Account does not participate in transaction confirmation.");
			}

			if (validationMode.getMaxWeight() == validationMode.getWeightForAccount(tx.getSenderID())) {
				return ValidationResult.error("Rejection is not possible.");
			}

		} catch (Exception e) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		return ValidationResult.success;
	}

}
