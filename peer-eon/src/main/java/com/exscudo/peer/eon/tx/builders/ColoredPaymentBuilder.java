package com.exscudo.peer.eon.tx.builders;

import com.exscudo.peer.core.TransactionType;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ColoredCoinID;

/**
 * "Colored Coin Payment" transaction.
 * <p>
 * Transfers colored coins between accounts.
 */
public class ColoredPaymentBuilder extends TransactionBuilder<ColoredPaymentBuilder> {

    private ColoredPaymentBuilder() {
        super(TransactionType.ColoredCoinPayment);
    }

    public static ColoredPaymentBuilder createNew(long amount, ColoredCoinID color, AccountID recipient) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount");
        }
        return new ColoredPaymentBuilder().withParam("amount", amount)
                                          .withParam("recipient", recipient.toString())
                                          .withParam("color", color.toString());
    }
}
