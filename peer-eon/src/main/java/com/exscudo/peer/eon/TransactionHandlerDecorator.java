package com.exscudo.peer.eon;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.TransactionContext;

/**
 * The class extends the functionality of the basic transaction handler.
 * </p>
 * The choice of the handler depending on the type of transaction.
 */
public class TransactionHandlerDecorator implements ITransactionHandler {

	private final Map<Integer, ITransactionHandler> dictionary = new HashMap<>();

	public TransactionHandlerDecorator() {
	}

	public TransactionHandlerDecorator(Map<Integer, ITransactionHandler> rules) {
		for (Map.Entry<Integer, ITransactionHandler> entry : rules.entrySet()) {
			this.bind(entry.getKey(), entry.getValue());
		}
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

	@Override
	public void run(Transaction transaction, ILedger ledger, TransactionContext context) throws ValidateException {
		Objects.requireNonNull(transaction);
		Objects.requireNonNull(context);

		ITransactionHandler handler = this.getItem(transaction.getType());
		if (handler == null) {
			throw new ValidateException("Invalid transaction type. Type :" + transaction.getType());
		}

		handler.run(transaction, ledger, context);
	}

}
