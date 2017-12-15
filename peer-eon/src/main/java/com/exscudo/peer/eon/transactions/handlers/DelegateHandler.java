package com.exscudo.peer.eon.transactions.handlers;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.state.Voter;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class DelegateHandler implements ITransactionHandler {

	@Override
	public void run(Transaction transaction, ILedger ledger, TransactionContext context) throws ValidateException {

		IAccount sender = ledger.getAccount(transaction.getSenderID());
		ValidationMode validationMode = AccountProperties.getValidationMode(sender);
		if (validationMode == null) {
			validationMode = new ValidationMode();
			validationMode.setBaseWeight(ValidationMode.MAX_WEIGHT);
		}

		final Map<String, Object> data = transaction.getData();
		if (data.size() < 1) {
			throw new IllegalArgumentException();
		}
		for (Map.Entry<String, Object> entry : data.entrySet()) {

			long id = Format.ID.accountId(entry.getKey());
			int weight = Integer.parseInt(String.valueOf(entry.getValue()));

			if (id == sender.getID()) {
				validationMode.setBaseWeight(weight);
			} else {

				IAccount target = ledger.getAccount(id);
				Voter targetVoter = AccountProperties.getVoter(target);
				if (targetVoter == null) {
					targetVoter = new Voter();
				}
				targetVoter.setPoll(transaction.getSenderID(), weight);
				targetVoter.setTimestamp(context.timestamp);
				AccountProperties.setVoter(target, targetVoter);
				ledger.putAccount(target);

				validationMode.setWeightForAccount(id, weight);
			}

		}

		validationMode.setTimestamp(context.timestamp);
		AccountProperties.setValidationMode(sender, validationMode);
		ledger.putAccount(sender);
	}
}
