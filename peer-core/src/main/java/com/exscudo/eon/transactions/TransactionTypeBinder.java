package com.exscudo.eon.transactions;

import java.util.HashMap;
import java.util.Map;

/**
 * Associates a transaction type with a handler.
 *
 * @param <THandler>
 *            Type of handler.
 */
public abstract class TransactionTypeBinder<THandler> {
	private final Map<Integer, THandler> dictionary = new HashMap<>();

	public synchronized void bind(TransactionType txType, THandler handler) {
		int key = txType.getKey();
		if (dictionary.containsKey(key)) {
			throw new IllegalArgumentException("The value is already in the dictionary.");
		}
		dictionary.put(key, handler);
	}

	protected synchronized THandler getItem(TransactionType type) {
		return dictionary.get(type.getKey());
	}

}
