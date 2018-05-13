package com.exscudo.peer.core.backlog.events;

import com.exscudo.peer.core.data.Transaction;

/**
 * An event generated as a result of a failure to add a
 * transaction to the Backlog.
 */
public class BacklogRejectEvent extends BacklogUpdateEvent {

    public final RejectionReason reason;

    public BacklogRejectEvent(Object source, Transaction transaction, RejectionReason reason) {
        super(source, transaction);

        this.reason = reason;
    }
}
