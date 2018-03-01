package com.exscudo.eon.bot;

import com.exscudo.peer.core.common.TimeProvider;

/**
 * Current peer time service
 */
public class TimeService {
    private final TimeProvider timeProvider;

    public TimeService(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    /**
     * Get current peer time service
     *
     * @return unix timestamp
     */
    public long get() {
        return timeProvider.get();
    }
}
