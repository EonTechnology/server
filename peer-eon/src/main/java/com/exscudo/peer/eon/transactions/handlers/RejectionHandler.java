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

public class RejectionHandler implements ITransactionHandler {

	@Override
	public void run(Transaction transaction, ILedger ledger, TransactionContext context) throws ValidateException {

		final Map<String, Object> data = transaction.getData();
		long id = Format.ID.accountId(String.valueOf(data.get("account")));

		IAccount sender = ledger.getAccount(transaction.getSenderID());
		Voter senderVoter = AccountProperties.getVoter(sender);
		if (senderVoter == null) {
			throw new IllegalStateException();
		}
		senderVoter.setPoll(id, 0);
		senderVoter.setTimestamp(context.timestamp);
		AccountProperties.setVoter(sender, senderVoter);
		ledger.putAccount(sender);

		IAccount target = ledger.getAccount(id);
		ValidationMode targetValidationMode = AccountProperties.getValidationMode(target);
		targetValidationMode.setWeightForAccount(transaction.getSenderID(), 0);
		targetValidationMode.setTimestamp(context.timestamp);
		AccountProperties.setValidationMode(target, targetValidationMode);
		ledger.putAccount(target);

	}

}
