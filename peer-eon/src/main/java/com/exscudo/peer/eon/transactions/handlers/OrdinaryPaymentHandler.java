package com.exscudo.peer.eon.transactions.handlers;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.transactions.rules.OrdinaryPaymentValidationRule;
import com.exscudo.peer.eon.transactions.utils.AccountBalance;

public class OrdinaryPaymentHandler extends BaseHandler {

	public OrdinaryPaymentHandler() {
		super(new OrdinaryPaymentValidationRule());
	}

	@Override
	protected void doRun(Transaction tx, ILedger ledger, TransactionContext context) {
		super.doRun(tx, ledger, context);

		Map<String, Object> data = tx.getData();

		long recipientID = Format.ID.accountId(data.get("recipient").toString());
		Long amount = Long.parseLong(data.get("amount").toString());

		IAccount sender = ledger.getAccount(tx.getSenderID());
		IAccount recipient = ledger.getAccount(recipientID);

		AccountBalance.withdraw(sender, amount);
		AccountBalance.refill(recipient, amount);

	}

}
