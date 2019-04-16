package org.eontechnology.and.peer.tx.midleware.builders;

import org.eontechnology.and.peer.tx.TransactionType;

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

    public static ColoredCoinSupplyBuilder createNew(String mode) {
        return new ColoredCoinSupplyBuilder().withParam("supply", mode);
    }
}
