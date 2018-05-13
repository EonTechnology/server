package com.exscudo.peer.tx.midleware.builders;

import com.exscudo.peer.tx.TransactionType;

/**
 * Transaction of management of the volume of issued colored coins
 */
public class ColoredCoinSupplyBuilder extends TransactionBuilder<ColoredCoinSupplyBuilder> {

    private ColoredCoinSupplyBuilder() {
        super(TransactionType.ColoredCoinSupply);
    }

    public static ColoredCoinSupplyBuilder createNew(long moneySupply) {
        return new ColoredCoinSupplyBuilder().withParam("supply", moneySupply);
    }

    public static ColoredCoinSupplyBuilder createNew() {
        return createNew(0L);
    }
}
