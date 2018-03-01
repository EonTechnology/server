package com.exscudo.peer.eon.ledger.state;

import com.exscudo.peer.eon.EonConstant;

public class BalanceProperty {
    private long value = 0L;

    public BalanceProperty() {

    }

    public BalanceProperty(long value) {
        setValue(value);
    }

    public long getValue() {
        return value;
    }

    public BalanceProperty setValue(long value) {
        ensureRange(value);
        this.value = value;
        return this;
    }

    public BalanceProperty refill(long amount) {
        long balance = getValue() + amount;
        setValue(balance);
        return this;
    }

    public BalanceProperty withdraw(long amount) {
        long balance = getValue() - amount;
        setValue(balance);
        return this;
    }

    private void ensureRange(long value) {
        if (value < 0 || value > EonConstant.MAX_MONEY) {
            throw new IllegalArgumentException("Illegal balance.");
        }
    }
}
