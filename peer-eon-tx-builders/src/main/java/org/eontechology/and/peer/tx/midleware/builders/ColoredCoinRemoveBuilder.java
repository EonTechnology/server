package org.eontechology.and.peer.tx.midleware.builders;

import org.eontechology.and.peer.tx.TransactionType;

/**
 * Removes a colored coin.
 */
public class ColoredCoinRemoveBuilder extends TransactionBuilder<ColoredCoinRemoveBuilder> {
    public ColoredCoinRemoveBuilder() {
        super(TransactionType.ColoredCoinRemove);
    }

    public static ColoredCoinRemoveBuilder createNew() {
        return new ColoredCoinRemoveBuilder();
    }
}
