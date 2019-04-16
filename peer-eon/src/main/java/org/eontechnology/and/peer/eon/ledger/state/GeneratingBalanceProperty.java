package org.eontechnology.and.peer.eon.ledger.state;

public class GeneratingBalanceProperty {
    private long deposit = 0L;
    private int timestamp = -1;

    public GeneratingBalanceProperty() {
    }

    public GeneratingBalanceProperty(long deposit, int timestamp) {
        this.deposit = deposit;
        this.timestamp = timestamp;
    }

    public long getValue() {
        return deposit;
    }

    public GeneratingBalanceProperty setValue(long deposit) {
        ensureRange(deposit);
        this.deposit = deposit;
        return this;
    }

    private void ensureRange(long value) {
        if (value < 0) {
            throw new IllegalArgumentException();
        }
    }

    public int getTimestamp() {
        return timestamp;
    }

    public GeneratingBalanceProperty setTimestamp(int timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}
