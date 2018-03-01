package com.exscudo.peer.eon.ledger.state;

/**
 * The properties of the colored coin
 */
public class ColoredCoinProperty {

    public static final int MIN_DECIMAL_POINT = 0;
    public static final int MAX_DECIMAL_POINT = 10;
    public static final long MIN_EMISSION_SIZE = 1L;

    private long moneySupply = 0L;
    private int decimalPoint = 0;
    private int timestamp = -1;

    public long getMoneySupply() {
        return moneySupply;
    }

    public ColoredCoinProperty setMoneySupply(long moneySupply) {
        ensureRange(moneySupply);
        this.moneySupply = moneySupply;
        return this;
    }

    private void ensureRange(long value) {
        if (value < 0) {
            throw new IllegalArgumentException();
        }
    }

    public int getDecimalPoint() {
        return decimalPoint;
    }

    public ColoredCoinProperty setDecimalPoint(int decimalPoint) {
        this.decimalPoint = decimalPoint;
        return this;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public ColoredCoinProperty setTimestamp(int timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public boolean isIssued() {
        return moneySupply != 0L;
    }
}
