package org.eontechology.and.peer.tx.midleware.builders;

import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.tx.ColoredCoinID;
import org.eontechology.and.peer.tx.TransactionType;

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
