package com.exscudo.peer.eon.transactions.builders;

import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.utils.ColoredCoinId;

/**
 * "Colored Coin Payment" transaction.
 * <p>
 * Transfers colored coins between accounts.
 */
public class ColoredPaymentBuilder extends TransactionBuilder<ColoredPaymentBuilder> {

	private ColoredPaymentBuilder() {
		super(TransactionType.ColoredCoinPayment);
	}

	public static ColoredPaymentBuilder createNew(long amount, long color, long recipient) {
		if (amount < 0) {
			throw new IllegalArgumentException("amount");
		}
		return new ColoredPaymentBuilder().withParam("amount", amount)
				.withParam("recipient", Format.ID.accountId(recipient))
				.withParam("color", ColoredCoinId.convert(color));
	}

}
