package com.exscudo.peer.eon.transactions.handlers;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.transactions.rules.DepositRefillValidationRule;
import com.exscudo.peer.eon.transactions.utils.AccountBalance;
import com.exscudo.peer.eon.transactions.utils.AccountDeposit;

public class DepositRefillHandler extends BaseHandler {

	public DepositRefillHandler() {
		super(new DepositRefillValidationRule());
	}

	@Override
	protected void doRun(Transaction tx, ILedger ledger, TransactionContext context) {
		super.doRun(tx, ledger, context);

		Long amount = Long.parseLong(tx.getData().get("amount").toString());

		IAccount account = ledger.getAccount(tx.getSenderID());
		AccountBalance.withdraw(account, amount);
		AccountDeposit.refill(account, amount);
	}

}
