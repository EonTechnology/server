package com.exscudo.peer.eon.transactions;

import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.state.ValidationMode;

/**
 * Specifies the weight for a particular account when using a multi-signature.
 */
public class Delegate {

	public static TransactionBuilder addAccount(long accountID, int weight) {
		if (weight < ValidationMode.MIN_WEIGHT || weight > ValidationMode.MAX_WEIGHT) {
			throw new IllegalArgumentException();
		}
		Map<String, Object> map = new HashMap<>();
		map.put(Format.ID.accountId(accountID), weight);
		return new TransactionBuilder(TransactionType.Delegate, map);
	}

	public static TransactionBuilder removeAccount(long accountID) {
		Map<String, Object> map = new HashMap<>();
		map.put(Format.ID.accountId(accountID), 0L);
		return new TransactionBuilder(TransactionType.Delegate, map);
	}

}
