package org.eontechnology.and.peer.eon.ledger.state;

/**
 * The properties of the colored coin
 */
public class ColoredCoinProperty {

    public static final int MIN_DECIMAL_POINT = 0;
    public static final int MAX_DECIMAL_POINT = 10;
    public static final long MIN_EMISSION_SIZE = 1L;

    private Attributes attributes = null;
    private long moneySupply = 0L;
    private ColoredCoinEmitMode emitMode = null;

    public Attributes getAttributes() {
        return attributes;
    }

    public ColoredCoinProperty setAttributes(Attributes attributes) {
        this.attributes = attributes;
        return this;
    }

    public long getMoneySupply() {
        return moneySupply;
    }

    public ColoredCoinProperty setMoneySupply(long moneySupply) {
        if (moneySupply < 0) {
            throw new IllegalArgumentException();
        }
        this.moneySupply = moneySupply;
        return this;
    }

    public ColoredCoinEmitMode getEmitMode() {
        return emitMode;
    }

    public ColoredCoinProperty setEmitMode(ColoredCoinEmitMode emitMode) {
        this.emitMode = emitMode;
        return this;
    }

    public boolean isIssued() {
        return attributes != null;
    }

    public static class Attributes {
        public final int decimalPoint;
        public final int timestamp;

        public Attributes(int decimalPoint, int timestamp) {
            this.decimalPoint = decimalPoint;
            this.timestamp = timestamp;
        }
    }
}
