package org.eontechnology.and.peer.core.env.events;

import java.util.EventListener;

/** This interface defines the list of events that occur when interacting with peers. */
public interface IPeerEventListener extends EventListener {

  /**
   * Gate to get a notification that the state was synchronized with services peer.
   *
   * @param event
   */
  void onSynchronized(PeerEvent event);
}
