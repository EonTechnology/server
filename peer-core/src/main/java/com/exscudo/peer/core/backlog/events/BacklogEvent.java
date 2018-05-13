package com.exscudo.peer.core.backlog.events;

import java.util.EventObject;

/***
 * This is the base class of all dispatched events which are associated with a
 * backlog.
 *
 */
public class BacklogEvent extends EventObject {

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public BacklogEvent(Object source) {
        super(source);
    }
}
