package com.exscudo.peer.core.services;

import com.exscudo.peer.core.Constant;

/**
 * Defines a set of parameters that are external to the state and transaction
 * and are necessary for processing it.
 *
 */
public class TransactionContext {
	public final int timestamp;
	public final long brokerID;

	public TransactionContext(int timestamp) {
		this(timestamp, Constant.DUMMY_ACCOUNT_ID);
	}

	public TransactionContext(int timestamp, long brokerID) {
		this.brokerID = brokerID;
		this.timestamp = timestamp;
	}
}
