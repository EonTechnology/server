package org.eontechnology.and.peer.core.common;

public class ImmutableTimeProvider implements ITimeProvider {
    private final int timestamp;

    public ImmutableTimeProvider(int timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int get() {
        return timestamp;
    }
}
