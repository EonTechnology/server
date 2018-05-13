package com.exscudo.peer.tx.midleware.builders;

import com.exscudo.peer.tx.TransactionType;

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
        return new ColoredCoinRegistrationBuilder().withParam("emission", emission).withParam("decimal", decimalPoint);
    }
}
