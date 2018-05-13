package com.exscudo.peer.core.backlog.events;

import java.util.EventListener;

/**
 * This interface defines the list of events that occur when interacting with
 * transactions.
 */
public interface IBacklogEventListener extends EventListener {

    /**
     * Gate to get a notification about the Backlog reset.
     *
     * @param event
     */
    void onClear(BacklogEvent event);

    /**
     * Gate to get a notification about the Backlog updating.
     *
     * @param event
     */
    void onUpdating(BacklogUpdateEvent event);

    /**
     * Gate to get a notification that the Backlog has been updated.
     *
     * @param event
     */
    void onUpdated(BacklogUpdateEvent event);

    /**
     * Gate to get a notification that the Backlog updating was rejected
     *
     * @param event
     */
    void onRejected(BacklogRejectEvent event);
}

