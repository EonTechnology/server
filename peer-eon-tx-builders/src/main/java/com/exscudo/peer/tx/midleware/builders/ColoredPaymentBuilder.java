package com.exscudo.peer.tx.midleware.builders;

import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.tx.ColoredCoinID;
import com.exscudo.peer.tx.TransactionType;

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
        return new ColoredPaymentBuilder().withParam("amount", amount)
                                          .withParam("recipient", recipient.toString())
                                          .withParam("color", color.toString());
    }
}
