package com.exscudo.peer.eon.tx.builders;

import com.exscudo.peer.core.TransactionType;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;

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
        if (emission < ColoredCoinProperty.MIN_EMISSION_SIZE) {
            throw new IllegalArgumentException("emission");
        }
        if (decimalPoint < ColoredCoinProperty.MIN_DECIMAL_POINT ||
                decimalPoint > ColoredCoinProperty.MAX_DECIMAL_POINT) {
            throw new IllegalArgumentException("decimalPoint");
        }
        return new ColoredCoinRegistrationBuilder().withParam("emission", emission)
                                                   .withParam("decimalPoint", decimalPoint);
    }
}
