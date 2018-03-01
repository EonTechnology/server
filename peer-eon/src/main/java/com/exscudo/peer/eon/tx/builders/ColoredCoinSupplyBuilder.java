package com.exscudo.peer.eon.tx.builders;

import com.exscudo.peer.core.TransactionType;

/**
 * Transaction of management of the volume of issued colored coins
 */
public class ColoredCoinSupplyBuilder extends TransactionBuilder<ColoredCoinSupplyBuilder> {

    private ColoredCoinSupplyBuilder() {
        super(TransactionType.ColoredCoinSupply);
    }

    public static ColoredCoinSupplyBuilder createNew(long moneySupply) {
        if (moneySupply < 0) {
            throw new IllegalArgumentException();
        }
        return new ColoredCoinSupplyBuilder().withParam("moneySupply", moneySupply);
    }

    public static ColoredCoinSupplyBuilder createNew() {
        return createNew(0L);
    }
}
