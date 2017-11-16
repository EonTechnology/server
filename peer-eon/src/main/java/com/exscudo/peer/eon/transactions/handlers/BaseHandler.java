package com.exscudo.peer.eon.transactions.handlers;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.transactions.rules.IValidationRule;
import com.exscudo.peer.eon.transactions.rules.ValidationResult;
import com.exscudo.peer.eon.transactions.utils.AccountBalance;

public class BaseHandler implements ITransactionHandler {
	private final IValidationRule rule;

	protected BaseHandler(IValidationRule rule) {
		this.rule = rule;
	}

	public void run(Transaction tx, ILedger ledger, TransactionContext context) throws ValidateException {

		ValidationResult r = rule.validate(tx, ledger, context);
		if (r.hasError) {
			throw r.cause;
		}

		doRun(tx, ledger, context);
	}

	protected void doRun(Transaction tx, ILedger ledger, TransactionContext context) {

		// update sender and broker balances
		AccountBalance.withdraw(ledger.getAccount(tx.getSenderID()), tx.getFee());
		AccountBalance.refill(ledger.getAccount(Constant.DUMMY_ACCOUNT_ID), tx.getFee());

	}

}
