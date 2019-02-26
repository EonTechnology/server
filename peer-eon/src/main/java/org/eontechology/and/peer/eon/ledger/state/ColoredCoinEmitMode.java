package org.eontechology.and.peer.eon.ledger.state;

public enum ColoredCoinEmitMode {
    AUTO("auto"),
    PRESET("preset");

    private final String value;

    ColoredCoinEmitMode(String value) {
        this.value = value;
    }

    public static ColoredCoinEmitMode fromString(String text) {
        for (ColoredCoinEmitMode m : ColoredCoinEmitMode.values()) {
            if (m.value.equalsIgnoreCase(text)) {
                return m;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}
