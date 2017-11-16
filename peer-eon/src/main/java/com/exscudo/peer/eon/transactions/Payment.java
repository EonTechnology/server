package com.exscudo.peer.eon.transactions;

import java.util.HashMap;

import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.TransactionType;

/**
 * "Payment" transaction.
 * <p>
 * Transfers coins between accounts.
 */
public class Payment {
	/**
	 * The maximum possible payment.
	 */
	public static final long MAX_PAYMENT = EonConstant.MAX_MONEY;

	/**
	 * Minimum allowable payment.
	 */
	public static final long MIN_PAYMENT = 0;

	public static TransactionBuilder newPayment(long amount, long recipient) {

		if (amount < MIN_PAYMENT || amount > MAX_PAYMENT) {
			throw new IllegalArgumentException("amount");
		}

		HashMap<String, Object> hashMap = new HashMap<>();
		hashMap.put("amount", amount);
		hashMap.put("recipient", Format.ID.accountId(recipient));

		return new TransactionBuilder(TransactionType.OrdinaryPayment, hashMap);

	}

}
