package com.exscudo.peer.eon.transactions.handlers;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class AccountPublicationHandler implements ITransactionHandler {

	@Override
	public void run(Transaction transaction, ILedger ledger, TransactionContext context) throws ValidateException {

		IAccount sender = ledger.getAccount(transaction.getSenderID());
		ValidationMode validationMode = AccountProperties.getValidationMode(sender);
		if (validationMode == null) {
			throw new IllegalStateException();
		}
		String seed = String.valueOf(transaction.getData().get("seed"));
		validationMode.setPublicMode(seed);
		validationMode.setTimestamp(context.timestamp);
		AccountProperties.setValidationMode(sender, validationMode);
		ledger.putAccount(sender);

	}

}
