package org.eontechnology.and.peer.core.env.events;

import java.util.EventObject;

import org.eontechnology.and.peer.core.env.Peer;

/***
 * This is the base class of all dispatched events which are associated with a
 * node.
 *
 */
public class PeerEvent extends EventObject {
    private static final long serialVersionUID = -5602705049652516067L;

    public final Peer peer;

    public PeerEvent(Object source, Peer peer) {
        super(source);
        this.peer = peer;
    }
}
