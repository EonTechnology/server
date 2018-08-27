package com.exscudo.peer.tx.midleware.builders;

import com.exscudo.peer.tx.TransactionType;

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
