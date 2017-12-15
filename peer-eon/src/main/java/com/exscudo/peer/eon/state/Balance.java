package com.exscudo.peer.eon.state;

import com.exscudo.peer.eon.EonConstant;

public class Balance {
	private long value;

	public Balance() {

	}

	public Balance(long value) {
		setValue(value);
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		ensureRange(value);
		this.value = value;
	}

	public void refill(long amount) {
		long balance = getValue() + amount;
		setValue(balance);
	}

	public void withdraw(long amount) {
		long balance = getValue() - amount;
		setValue(balance);
	}

	private void ensureRange(long value) {
		if (value < 0 || value > EonConstant.MAX_MONEY) {
			throw new IllegalArgumentException("Illegal balance.");
		}
	}

}
