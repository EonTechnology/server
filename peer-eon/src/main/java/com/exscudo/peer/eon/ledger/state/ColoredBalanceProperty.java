package com.exscudo.peer.eon.ledger.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.exscudo.peer.eon.ColoredCoinID;

public class ColoredBalanceProperty {

    /**
     * Contains a list of balances by colors.
     */
    private Map<ColoredCoinID, Long> coloredBalances = new HashMap<>();

    public void setBalance(long amount, ColoredCoinID color) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount");
        }
        coloredBalances.put(color, amount);
    }

    public long getBalance(ColoredCoinID color) {
        Long amount = coloredBalances.get(color);
        return (amount == null) ? 0 : amount;
    }

    public ColoredBalanceProperty refill(long amount, ColoredCoinID color) {
        long balance = getBalance(color) + amount;
        setBalance(balance, color);
        return this;
    }

    public ColoredBalanceProperty withdraw(long amount, ColoredCoinID color) {
        long balance = getBalance(color) - amount;
        setBalance(balance, color);
        return this;
    }

    public Set<Map.Entry<ColoredCoinID, Long>> balancesEntrySet() {
        return coloredBalances.entrySet();
    }
}
