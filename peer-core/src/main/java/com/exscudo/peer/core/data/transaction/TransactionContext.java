package com.exscudo.peer.core.data.transaction;

/**
 * Defines a set of parameters that are external to the state and transaction
 * and are necessary for processing it.
 */
public class TransactionContext {
    private final int timestamp;

    public TransactionContext(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getTimestamp() {
        return timestamp;
    }
}
