package com.exscudo.peer.core.services;

/**
 * Defines a set of parameters that are external to the state and transaction
 * and are necessary for processing it.
 */
public class TransactionContext {
	public final int timestamp;
	public final int height;
	public long totalFee;

	public TransactionContext(int timestamp, int height) {
		this.timestamp = timestamp;
		this.height = height;
	}

	public void setTotalFee(long fee) {
		totalFee = fee;
	}

	public long getTotalFee() {
		return totalFee;
	}
}
