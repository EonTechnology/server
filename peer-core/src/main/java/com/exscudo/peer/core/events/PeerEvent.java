package com.exscudo.peer.core.events;

import java.util.EventObject;

import com.exscudo.peer.core.IPeer;

/***
 * This is the base class of all dispatched events which are associated with a
 * node.
 *
 */
public class PeerEvent extends EventObject {
	private static final long serialVersionUID = -5602705049652516067L;

	public final IPeer peer;

	public PeerEvent(Object source, IPeer peer) {
		super(source);
		this.peer = peer;
	}
}
