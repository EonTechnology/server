package com.exscudo.peer.eon.transactions.rules;

import java.util.HashSet;
import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.transactions.utils.AccountAttributes;

public class AccountRegistrationValidationRule extends BaseValidationRule {

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
		if (data == null || data.size() != 1) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		try {
			HashSet<Long> accsInTr = new HashSet<>();
			for (Map.Entry<String, Object> entry : data.entrySet()) {

				long accID = Format.ID.accountId(entry.getKey());
				if (!(entry.getValue() instanceof String)) {
					return ValidationResult.error("Attachment of unknown type.");
				}

				byte[] publicKey = Format.convert(entry.getValue().toString());
				if (accID != Format.MathID.pick(publicKey)) {
					return ValidationResult.error("Attachment of unknown type.");
				}

				if (publicKey.length != AccountAttributes.getPublicKey(sender).length) {
					return ValidationResult.error("Attachment of unknown type.");
				}

				IAccount account = ledger.getAccount(accID);
				if (account != null) {
					return ValidationResult.error("IAccount can not be to created.");
				}

				if (accsInTr.contains(accID)) {
					return ValidationResult.error("IAccount can not be to created.");
				}
				accsInTr.add(accID);
			}
		} catch (Exception e) {
			return ValidationResult.error("Attachment of unknown type.");
		}

		return ValidationResult.success;
	}

}
