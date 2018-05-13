package com.exscudo.peer.core.middleware;

import com.exscudo.peer.core.IFork;

/**
 * Defines a set of parameters that are external to the state and actions
 * and are necessary for processing it.
 */
public class LedgerActionContext {
    private final int timestamp;
    //TODO: need review
    public final IFork fork;

    public LedgerActionContext(int timestamp, IFork fork) {
        this.timestamp = timestamp;
        this.fork = fork;
    }

    public int getTimestamp() {
        return timestamp;
    }
}
