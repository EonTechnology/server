package com.exscudo.peer.eon.transactions.rules;

import java.util.HashSet;
import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class AccountRegistrationValidationRule implements IValidationRule {

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

			HashSet<Long> set = new HashSet<>();
			for (Map.Entry<String, Object> entry : data.entrySet()) {

				long id = Format.ID.accountId(entry.getKey());
				byte[] publicKey = Format.convert(String.valueOf(entry.getValue()));

				if (id != Format.MathID.pick(publicKey)) {
					return ValidationResult.error("Attachment of unknown type.");
				}

				if (publicKey.length != AccountProperties.getPublicKey(sender).length) {
					return ValidationResult.error("Attachment of unknown type.");
				}

				IAccount account = ledger.getAccount(id);
				if (account != null) {
					return ValidationResult.error("Account already exists.");
				}

				if (set.contains(id)) {
					return ValidationResult.error("Account is duplicated.");
				}
				set.add(id);
			}

		} catch (Exception e) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		return ValidationResult.success;
	}

}
