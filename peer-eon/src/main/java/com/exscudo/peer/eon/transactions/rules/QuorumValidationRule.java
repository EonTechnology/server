package com.exscudo.peer.eon.transactions.rules;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class QuorumValidationRule implements IValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		Map<String, Object> data = tx.getData();
		if (data == null || data.size() == 0) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		IAccount sender = ledger.getAccount(tx.getSenderID());
		if (sender == null) {
			return ValidationResult.error("Unknown sender.");
		}

		int maxWeight = ValidationMode.MAX_WEIGHT;
		ValidationMode validationMode = AccountProperties.getValidationMode(sender);
		if (validationMode != null && validationMode.isMultiFactor()) {
			maxWeight = validationMode.getMaxWeight();
		}

		try {

			int quorum = Integer.parseInt(String.valueOf(data.get("all")));
			if (quorum < ValidationMode.MIN_QUORUM || quorum > ValidationMode.MAX_QUORUM) {
				return ValidationResult.error("Illegal quorum.");
			}
			if (maxWeight < quorum) {
				return ValidationResult.error("Unable to set quorum.");
			}

			for (Map.Entry<String, Object> e : data.entrySet()) {

				if (e.getKey().equals("all")) {
					continue;
				}

				int type = Integer.parseInt(String.valueOf(e.getKey()));
				int quorumTyped = Integer.parseInt(String.valueOf(e.getValue()));

				if (!TransactionType.contains(type)) {
					return ValidationResult.error("Unknown transaction type " + e.getKey());
				}
				if (quorumTyped < ValidationMode.MIN_QUORUM || quorumTyped > ValidationMode.MAX_QUORUM) {
					return ValidationResult.error("Illegal quorum for transaction type " + e.getKey());
				}
				if (maxWeight < quorumTyped) {
					return ValidationResult.error("Unable to set quorum for transaction type " + e.getKey());
				}
				if (quorumTyped == quorum) {
					return ValidationResult.error("Use all quorum for transaction type " + e.getKey());
				}

			}

		} catch (Exception e) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		return ValidationResult.success;
	}

}
