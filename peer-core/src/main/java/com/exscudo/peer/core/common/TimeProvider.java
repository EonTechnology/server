package com.exscudo.peer.core.common;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Default time provider implementation.
 */
public class TimeProvider implements ITimeProvider {

    private AtomicLong offsetTime = new AtomicLong(0);

    /**
     * Corrects the time offset if the value difference is more than minimal step.
     *
     * @param currOffset
     * @return
     */
    public synchronized boolean trySetTimeOffset(long currOffset) {

        if (Math.abs(currOffset - offsetTime.get()) > 1000) {

            offsetTime.set(currOffset);
            return true;
        }

        return false;
    }

    /**
     * Provides access to the current time.
     *
     * @return
     */
    @Override
    public int get() {
        long time = System.currentTimeMillis() + offsetTime.get();
        return (int) ((time + 500) / 1000);
    }
}
