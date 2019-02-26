package org.eontechology.and.peer.core.middleware;

/**
 * Defines a set of parameters that are external to the state and actions
 * and are necessary for processing it.
 */
public class LedgerActionContext {
    private final int timestamp;

    public LedgerActionContext(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getTimestamp() {
        return timestamp;
    }
}
