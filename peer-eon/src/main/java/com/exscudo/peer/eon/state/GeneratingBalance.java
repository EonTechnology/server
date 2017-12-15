package com.exscudo.peer.eon.state;

import com.exscudo.peer.eon.EonConstant;

public class GeneratingBalance {
	private long deposit;
	private int height;

	public GeneratingBalance() {
	}

	public GeneratingBalance(long deposit, int height) {
		this.deposit = deposit;
		this.height = height;
	}

	public long getValue() {
		return deposit;
	}

	public void setValue(long deposit) {
		ensureRange(deposit);
		this.deposit = deposit;
	}

	private void ensureRange(long value) {
		if (value < 0 || value > EonConstant.MAX_MONEY) {
			throw new IllegalArgumentException();
		}
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void refill(long amount) {
		if (amount <= 0 || amount > EonConstant.MAX_MONEY) {
			throw new IllegalArgumentException("amount");
		}
		long newDeposit = getValue() + amount;
		setValue(newDeposit);
	}

	public void withdraw(long amount) {
		if (amount <= 0 || amount > EonConstant.MAX_MONEY) {
			throw new IllegalArgumentException("amount");
		}
		long newDeposit = getValue() - amount;
		setValue(newDeposit);
	}

}
