package com.exscudo.peer.eon.transactions.builders;

import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.state.ColoredCoin;

/**
 * Creates a colored coins.
 * <p>
 * Performs binding of a colored coin to an existing account.
 */
public class ColoredCoinRegistrationBuilder extends TransactionBuilder<ColoredCoinRegistrationBuilder> {

	private ColoredCoinRegistrationBuilder() {
		super(TransactionType.ColoredCoinRegistration);
	}

	public static ColoredCoinRegistrationBuilder createNew(long emission, int decimalPoint) {
		if (emission < ColoredCoin.MIN_EMISSION_SIZE) {
			throw new IllegalArgumentException("emission");
		}
		if (decimalPoint < ColoredCoin.MIN_DECIMAL_POINT || decimalPoint > ColoredCoin.MAX_DECIMAL_POINT) {
			throw new IllegalArgumentException("decimalPoint");
		}
		return new ColoredCoinRegistrationBuilder().withParam("emission", emission).withParam("decimalPoint",
				decimalPoint);
	}
}
