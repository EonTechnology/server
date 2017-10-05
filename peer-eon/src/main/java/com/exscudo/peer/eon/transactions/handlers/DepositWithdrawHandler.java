package com.exscudo.peer.eon.transactions.handlers;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.transactions.rules.DepositWithdrawValidationRule;
import com.exscudo.peer.eon.transactions.utils.AccountBalance;
import com.exscudo.peer.eon.transactions.utils.AccountDeposit;

public class DepositWithdrawHandler extends BaseHandler {

	public DepositWithdrawHandler() {
		super(new DepositWithdrawValidationRule());
	}

	@Override
	protected void doRun(Transaction tx, ILedger ledger, TransactionContext context) {
		super.doRun(tx, ledger, context);

		Long amount = Long.parseLong(tx.getData().get("amount").toString());
		IAccount account = ledger.getAccount(tx.getSenderID());

		AccountDeposit.withdraw(account, amount);
		AccountBalance.refill(account, amount);
	}

}
