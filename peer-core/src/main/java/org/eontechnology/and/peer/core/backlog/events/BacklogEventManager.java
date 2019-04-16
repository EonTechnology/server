package org.eontechnology.and.peer.core.backlog.events;

import java.util.Objects;

import org.eontechnology.and.peer.core.common.events.DispatchableEvent;
import org.eontechnology.and.peer.core.common.events.Dispatcher;
import org.eontechnology.and.peer.core.data.Transaction;

/**
 * Basic implementation of the manager for Backlog events.
 */
public class BacklogEventManager {

    private final BacklogEventSupport backlogEventSupport = new BacklogEventSupport();

    public void addListener(IBacklogEventListener listener) {
        Objects.requireNonNull(listener);
        backlogEventSupport.addListener(listener);
    }

    public void removeListener(IBacklogEventListener listener) {
        Objects.requireNonNull(listener);
        backlogEventSupport.removeListener(listener);
    }

    public void raiseClear(Object source) {
        backlogEventSupport.raiseEvent(new DispatchableEvent<IBacklogEventListener, BacklogEvent>(new BacklogEvent(
                source)) {
            @Override
            public void dispatch(IBacklogEventListener target, BacklogEvent event) {
                target.onClear(event);
            }
        });
    }

    public void raiseUpdating(Object source, Transaction transaction) {
        backlogEventSupport.raiseEvent(new DispatchableEvent<IBacklogEventListener, BacklogUpdateEvent>(new BacklogUpdateEvent(
                source,
                transaction)) {
            @Override
            public void dispatch(IBacklogEventListener target, BacklogUpdateEvent event) {
                target.onUpdating(event);
            }
        });
    }

    public void raiseUpdated(Object source, Transaction transaction) {
        backlogEventSupport.raiseEvent(new DispatchableEvent<IBacklogEventListener, BacklogUpdateEvent>(new BacklogUpdateEvent(
                source,
                transaction)) {
            @Override
            public void dispatch(IBacklogEventListener target, BacklogUpdateEvent event) {
                target.onUpdated(event);
            }
        });
    }

    public void raiseRejected(Object source, Transaction transaction, RejectionReason reason) {
        backlogEventSupport.raiseEvent(new DispatchableEvent<IBacklogEventListener, BacklogRejectEvent>(new BacklogRejectEvent(
                source,
                transaction,
                reason)) {
            @Override
            public void dispatch(IBacklogEventListener target, BacklogRejectEvent event) {
                target.onRejected(event);
            }
        });
    }

    private static class BacklogEventSupport extends Dispatcher<IBacklogEventListener> {
        public BacklogEventSupport() {
            super();
        }
    }
}
