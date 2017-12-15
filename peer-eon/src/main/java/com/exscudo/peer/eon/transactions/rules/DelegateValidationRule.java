package com.exscudo.peer.eon.transactions.rules;

import java.util.HashSet;
import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class DelegateValidationRule implements IValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		final Map<String, Object> data = tx.getData();
		if (data == null || data.size() != 1) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		try {

			IAccount sender = ledger.getAccount(tx.getSenderID());
			if (sender == null) {
				return ValidationResult.error("Unknown sender.");
			}

			ValidationMode validationMode = AccountProperties.getValidationMode(sender);
			if (validationMode == null) {
				validationMode = new ValidationMode();
				validationMode.setBaseWeight(ValidationMode.MAX_WEIGHT);
			}

			HashSet<Long> set = new HashSet<>();
			int deltaWeight = 0;
			for (Map.Entry<String, Object> entry : data.entrySet()) {

				long id = Format.ID.accountId(entry.getKey());
				int weight = Integer.parseInt(String.valueOf(entry.getValue()));

				if (set.contains(id)) {
					return ValidationResult.error("Invalid attachment. Duplicates.");
				}
				if (id == tx.getSenderID()) {

					// cancellation of rights is not possible
					if (validationMode.isPublic()) {
						return ValidationResult.error("Changing rights is prohibited.");
					}

					// invalid value
					if (weight < ValidationMode.MIN_WEIGHT || weight > ValidationMode.MAX_WEIGHT) {
						return ValidationResult.error("Invalid " + entry.getKey() + " account weight.");
					}

					if (weight == validationMode.getBaseWeight()) {
						return ValidationResult.error("Value already set.");
					}

					deltaWeight += weight - validationMode.getBaseWeight();

				} else {

					IAccount account = ledger.getAccount(id);
					if (account == null) {
						return ValidationResult.error("Unknown account " + entry.getKey());
					}
					ValidationMode accountValidationMode = AccountProperties.getValidationMode(account);
					if(accountValidationMode != null) {
						if (accountValidationMode.isPublic()) {
							return ValidationResult.error("A public account can not act as a delegate.");
						}
					}

					// invalid value
					if (weight < ValidationMode.MIN_WEIGHT || weight > ValidationMode.MAX_WEIGHT) {
						return ValidationResult.error("Invalid " + entry.getKey() + " account weight.");
					}

					if (validationMode.containWeightForAccount(id)) {
						weight = weight - validationMode.getWeightForAccount(id);
					}
					deltaWeight += weight;

				}
				set.add(id);

			}

			if ((validationMode.getMaxWeight() + deltaWeight) < ValidationMode.MAX_QUORUM) {
				return ValidationResult.error("Incorrect distribution of votes.");
			}

		} catch (Exception e) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		return ValidationResult.success;
	}

}
