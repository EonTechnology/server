package com.exscudo.peer.eon.transactions.builders;

import java.util.HashMap;

import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.TransactionType;

/**
 * "Payment" transaction.
 * <p>
 * Transfers coins between accounts.
 */
public class PaymentBuilder extends TransactionBuilder<PaymentBuilder> {
	/**
	 * The maximum possible payment.
	 */
	public static final long MAX_PAYMENT = EonConstant.MAX_MONEY;

	/**
	 * Minimum allowable payment.
	 */
	public static final long MIN_PAYMENT = 0;

	private PaymentBuilder() {
		super(TransactionType.OrdinaryPayment, new HashMap<>());
	}

	public static PaymentBuilder createNew(long amount, long recipient) {
		if (amount < MIN_PAYMENT || amount > MAX_PAYMENT) {
			throw new IllegalArgumentException("amount");
		}
		return new PaymentBuilder().withParam("amount", amount).withParam("recipient", Format.ID.accountId(recipient));
	}

}
