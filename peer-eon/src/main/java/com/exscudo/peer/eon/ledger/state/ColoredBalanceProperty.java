package com.exscudo.peer.eon.ledger.state;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.exscudo.peer.eon.ColoredCoinID;

public class ColoredBalanceProperty implements Iterable<ColoredCoinID> {

    /**
     * Contains a list of balances by colors.
     */
    private Map<String, Object> coloredBalances;

    public ColoredBalanceProperty() {
        coloredBalances = new HashMap<>();
    }

    public ColoredBalanceProperty(Map<String, Object> ext) {
        coloredBalances = new TreeMap<>(ext);
    }

    public void setBalance(long amount, ColoredCoinID color) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount");
        }
        if (amount != 0) {
            coloredBalances.put(color.toString(), amount);
        } else {
            coloredBalances.remove(color.toString());
        }
    }

    public long getBalance(ColoredCoinID color) {
        Object amount = coloredBalances.get(color.toString());
        return (amount == null) ? 0 : Long.parseLong(amount.toString());
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

    public Map<String, Object> getProperty() {
        return coloredBalances;
    }

    @Override
    public Iterator<ColoredCoinID> iterator() {

        final Iterator<String> iterator = coloredBalances.keySet().iterator();

        return new Iterator<ColoredCoinID>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public ColoredCoinID next() {
                return new ColoredCoinID(iterator.next());
            }
        };
    }
}
