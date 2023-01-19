package org.eontechnology.and.peer.core.common.events;

import java.util.EventObject;
import java.util.concurrent.Executor;

/**
 * Delivers the event to a specific listener using the specified delivery mechanism.
 *
 * @param <TTarget> type of the listener
 * @param <TEvent> type of the event
 */
public abstract class DispatchableEvent<TTarget, TEvent extends EventObject> {

  private final TEvent event;

  public DispatchableEvent(TEvent event) {
    this.event = event;
  }

  void dispatch(Executor executor, final TTarget target) {

    executor.execute(
        new Runnable() {
          public void run() {
            try {
              dispatch(target, event);
            } catch (Exception ignore) {
              // WARNING: error handling inside the dispatcher
            }
          }
        });
  }

  public abstract void dispatch(TTarget target, TEvent event);
}
