package com.exscudo.peer.eon;

import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;
import com.exscudo.peer.eon.transactions.rules.IValidationRule;
import com.exscudo.peer.eon.transactions.rules.ValidationResult;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;

public class TransactionHandler implements ITransactionHandler {
	private final Map<Integer, ITransactionHandler> dictionary = new HashMap<>();
	private final IValidationRule[] rules;

	public TransactionHandler(Map<Integer, ITransactionHandler> handlers, IValidationRule[] rule) {
		for (Map.Entry<Integer, ITransactionHandler> entry : handlers.entrySet()) {
			this.bind(entry.getKey(), entry.getValue());
		}
		this.rules = rule;
	}

	public synchronized void bind(int txType, ITransactionHandler handler) {
		if (dictionary.containsKey(txType)) {
			throw new IllegalArgumentException("The value is already in the dictionary.");
		}
		dictionary.put(txType, handler);
	}

	private synchronized ITransactionHandler getItem(int type) {
		return dictionary.get(type);
	}

	public void run(Transaction transaction, ILedger ledger, TransactionContext context) throws ValidateException {

		for (IValidationRule rule : rules) {
			ValidationResult r = rule.validate(transaction, ledger, context);
			if (r.hasError) {
				throw r.cause;
			}
		}

		// update sender balances
		IAccount sender = ledger.getAccount(transaction.getSenderID());
		AccountProperties.balanceWithdraw(sender, transaction.getFee());
		ledger.putAccount(sender);

		// attach handler
		ITransactionHandler attachHandler = this.getItem(transaction.getType());
		if (attachHandler == null) {
			throw new ValidateException("Invalid transaction type. Type :" + transaction.getType());
		}
		attachHandler.run(transaction, ledger, context);

		// update fee
		context.setTotalFee(context.getTotalFee() + transaction.getFee());
	}

}
