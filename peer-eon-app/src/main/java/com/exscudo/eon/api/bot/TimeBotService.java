package com.exscudo.eon.api.bot;

import com.exscudo.peer.core.common.TimeProvider;

/**
 * Current peer time service
 */
public class TimeBotService {
    private final TimeProvider timeProvider;

    public TimeBotService(TimeProvider timeProvider) {
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
