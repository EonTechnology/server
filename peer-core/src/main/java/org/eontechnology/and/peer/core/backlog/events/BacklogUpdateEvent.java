package org.eontechnology.and.peer.core.backlog.events;

import org.eontechnology.and.peer.core.data.Transaction;

/**
 * Event associated with adding a transaction to the Backlog.
 */
public class BacklogUpdateEvent extends BacklogEvent {
    public final Transaction transaction;

    public BacklogUpdateEvent(Object source, Transaction transaction) {
        super(source);

        this.transaction = transaction;
    }
}
