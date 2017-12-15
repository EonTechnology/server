package com.exscudo.peer.eon.transactions.handlers;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class DepositRefillHandler implements ITransactionHandler {

	@Override
	public void run(Transaction tx, ILedger ledger, TransactionContext context) {

		Long amount = Long.parseLong(tx.getData().get("amount").toString());

		IAccount account = ledger.getAccount(tx.getSenderID());
		AccountProperties.balanceWithdraw(account, amount);
		AccountProperties.depositRefill(account, amount, context.height);
		ledger.putAccount(account);

	}

}
