package com.exscudo.peer.eon.transactions.handlers;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class QuorumHandler implements ITransactionHandler {

	@Override
	public void run(Transaction transaction, ILedger ledger, TransactionContext context) throws ValidateException {

		IAccount account = ledger.getAccount(transaction.getSenderID());
		ValidationMode validationMode = AccountProperties.getValidationMode(account);
		if(validationMode == null){
			validationMode = new ValidationMode();
			validationMode.setBaseWeight(ValidationMode.MAX_WEIGHT);
		}

		Map<String, Object> data = transaction.getData();
		if(data.size() < 1) {
			throw new IllegalArgumentException();
		}
		for (Map.Entry<String, Object> entry : data.entrySet()) {

			if (entry.getKey().equals("all")) {
				int quorum = Integer.parseInt(String.valueOf(entry.getValue()));
				validationMode.setQuorum(quorum);
			} else {
				String k = String.valueOf(entry.getKey());
				String v = String.valueOf(entry.getValue());

				validationMode.setQuorum(Integer.parseInt(k), Integer.parseInt(v));
			}
		}

		validationMode.setTimestamp(context.timestamp);
		AccountProperties.setValidationMode(account, validationMode);
		ledger.putAccount(account);

	}

}
