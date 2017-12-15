package com.exscudo.peer.eon.transactions.rules;

import java.util.HashSet;
import java.util.Map;

import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.IllegalSignatureException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class ConfirmationsValidationRule implements IValidationRule {

	@Override
	public ValidationResult validate(Transaction tx, ILedger ledger, TransactionContext context) {

		IAccount sender = ledger.getAccount(tx.getSenderID());
		if (sender == null) {
			return ValidationResult.error("Unknown sender.");
		}

		ValidationMode validationMode = AccountProperties.getValidationMode(sender);
		if (validationMode != null && validationMode.isMultiFactor()) {

			HashSet<Long> set = new HashSet<>();
			set.add(tx.getSenderID());
			int maxWeight = validationMode.getBaseWeight();

			byte[] message = tx.getBytes();
			if (tx.getConfirmations() != null) {
				for (Map.Entry<String, Object> e : tx.getConfirmations().entrySet()) {

					long id = Format.ID.accountId(e.getKey());
					if (set.contains(id)) {
						return ValidationResult.error("Duplicates.");
					}
					IAccount account = ledger.getAccount(id);
					if (account == null) {
						return ValidationResult.error("Unknown account " + e.getKey());
					}

					if (validationMode.containWeightForAccount(id)) {
						maxWeight += validationMode.getWeightForAccount(id);
					} else {
						return ValidationResult.error("Account '" + e.getKey() + "' can not sign transaction.");
					}

					byte[] publicKey = AccountProperties.getPublicKey(account);
					byte[] signature = Format.convert(String.valueOf(e.getValue()));
					if (!CryptoProvider.getInstance().verifySignature(message, signature, publicKey)) {
						return ValidationResult.error(new IllegalSignatureException(e.getKey()));
					}

					set.add(id);
				}
			}

			if (validationMode.quorumForType(tx.getType()) <= maxWeight
					|| (maxWeight == validationMode.getMaxWeight() && maxWeight != 0)) {
				return ValidationResult.success;
			}

			return ValidationResult.error("The quorum is not exist.");
		} else {
			if (tx.getConfirmations() != null) {
				return ValidationResult.error("Invalid use of the confirmation field.");
			}
			return ValidationResult.success;
		}

	}

}
