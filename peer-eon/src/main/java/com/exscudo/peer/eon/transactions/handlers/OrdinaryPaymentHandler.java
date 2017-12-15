package com.exscudo.peer.eon.transactions.handlers;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class OrdinaryPaymentHandler implements ITransactionHandler {

	@Override
	public void run(Transaction tx, ILedger ledger, TransactionContext context) {

		Map<String, Object> data = tx.getData();

		long recipientID = Format.ID.accountId(data.get("recipient").toString());
		Long amount = Long.parseLong(data.get("amount").toString());

		IAccount sender = ledger.getAccount(tx.getSenderID());
		AccountProperties.balanceWithdraw(sender, amount);
		ledger.putAccount(sender);

		IAccount recipient = ledger.getAccount(recipientID);
		AccountProperties.balanceRefill(recipient, amount);
		ledger.putAccount(recipient);

	}

}
