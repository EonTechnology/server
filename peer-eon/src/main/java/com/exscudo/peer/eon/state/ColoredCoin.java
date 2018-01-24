package com.exscudo.peer.eon.state;

/**
 * The properties of the colored coin
 */
public class ColoredCoin {

	public static final int MIN_DECIMAL_POINT = 0;
	public static final int MAX_DECIMAL_POINT = 10;
	public static final long MIN_EMISSION_SIZE = 1L;

	private long moneySupply;
	private int decimalPoint;
	private int timestamp;

	public long getMoneySupply() {
		return moneySupply;
	}

	public void setMoneySupply(long moneySupply) {
		ensureRange(moneySupply);
		this.moneySupply = moneySupply;
	}

	private void ensureRange(long value) {
		if (value < 0) {
			throw new IllegalArgumentException();
		}
	}

	public int getDecimalPoint() {
		return decimalPoint;
	}

	public void setDecimalPoint(int decimalPoint) {
		this.decimalPoint = decimalPoint;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
}
