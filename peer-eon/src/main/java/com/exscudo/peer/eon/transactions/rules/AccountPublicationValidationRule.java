package com.exscudo.peer.eon.transactions.rules;

import java.util.Map;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.crypto.Ed25519Signer;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.state.Voter;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class AccountPublicationValidationRule implements IValidationRule {

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

			String seed = String.valueOf(data.get("seed"));
			byte[] publicKey = new Ed25519Signer(seed).getPublicKey();
			if (tx.getSenderID() != Format.MathID.pick(publicKey)) {
				return ValidationResult.error("Seed for sender account must be specified in attachment.");
			}

		} catch (Exception e) {
			return ValidationResult.error("Invalid seed.");
		}

		ValidationMode validationMode = AccountProperties.getValidationMode(sender);
		if (validationMode == null) {
			return ValidationResult.error("Invalid use of transaction.");
		}
		if (validationMode.isPublic()) {
			return ValidationResult.error("Already public.");
		}
		if (validationMode.getBaseWeight() != ValidationMode.MIN_WEIGHT
				|| validationMode.getMaxWeight() == validationMode.getBaseWeight()) {
			return ValidationResult
					.error("Illegal validation mode. Do not use this seed more for personal operations.");
		}

		Voter voter = AccountProperties.getVoter(sender);
		if(voter == null) {
			voter = new Voter();
		}

		if(voter.hasPolls()) {
			return ValidationResult.error("A public account must not confirm transactions of other accounts."
					+ " Do not use this seed more for personal operations.");
		}

		int timestamp = context.timestamp - Constant.BLOCK_IN_DAY * Constant.BLOCK_PERIOD;
		if (validationMode.getTimestamp() > timestamp || voter.getTimestamp() > timestamp) {
			return ValidationResult.error("The confirmation mode were changed earlier than a day ago."
					+ " Do not use this seed more for personal operations.");
		}


		return ValidationResult.success;
	}

}
