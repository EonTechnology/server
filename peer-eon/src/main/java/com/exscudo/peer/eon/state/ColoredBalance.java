package com.exscudo.peer.eon.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ColoredBalance {

	/**
	 * Contains a list of balances by colors.
	 */
	private Map<Long, Long> coloredBalances = new HashMap<>();

	public void setBalance(long amount, long color) {
		if (amount < 0) {
			throw new IllegalArgumentException("amount");
		}
		coloredBalances.put(color, amount);
	}

	public long getBalance(long color) {
		Long amount = coloredBalances.get(color);
		return (amount == null) ? 0 : amount;
	}

	public void refill(long amount, long color) {
		long balance = getBalance(color) + amount;
		setBalance(balance, color);
	}

	public void withdraw(long amount, long color) {
		long balance = getBalance(color) - amount;
		setBalance(balance, color);
	}

	public Set<Map.Entry<Long, Long>> balancesEntrySet() {
		return coloredBalances.entrySet();
	}

}
