package org.eontechology.and.eon.app.api.bot;

import org.eontechology.and.peer.core.common.ITimeProvider;

/**
 * Current peer time service
 */
public class TimeBotService {
    private final ITimeProvider timeProvider;

    public TimeBotService(ITimeProvider timeProvider) {
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
