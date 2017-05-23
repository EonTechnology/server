package com.exscudo.eon.peer;

/**
 * The class <code>Peer</code> used to storage the state and to describe
 * configuration of a remote node.
 *
 */
public class Peer implements Comparable<Peer> {

	//
	// STATE_AMBIGUOUS
	// The node has an unknown state.
	// 1. The initial state of the node.
	// 2. In this state the node gets after exclusion from the black list.
	//
	// STATE_CONNECTED
	// The node is in the pool of active nodes (used to synchronize data)
	//
	// STATE_DISCONNECTED
	// In this state, the node falls if it does not respond, or use
	// unsupported protocol.
	//
	public static enum State {

		STATE_AMBIGUOUS, STATE_CONNECTED, STATE_DISCONNECTED;
	}

	//
	// The configuration of a remote host.
	//
	public static class Metadata {

		/**
		 * A random number generated at node startup.
		 */
		public final long peerID;

		/**
		 * Application identifier.
		 */
		public final String application;

		/**
		 * Version of the node.
		 */
		public final String version;

		//
		// TODO hallmark and services list
		//

		public Metadata(long peerID, String application, String version) {

			this.peerID = peerID;
			this.application = application;
			this.version = version;

		}
	}

	final int index;
	final String announcedAddress;

	//
	// TODO The host address must not be distributed over the network. Not yet
	// implement.
	//
	public final boolean inner;

	private volatile State state;
	private volatile long blacklistingTime;
	private volatile Metadata metadata;

	Peer(int index, String announcedAddress, boolean inner) {

		this.announcedAddress = announcedAddress;
		this.index = index;
		this.inner = inner;

		state = State.STATE_AMBIGUOUS;
		metadata = new Metadata(0, null, null);

	}

	public String getAnnouncedAddress() {
		return announcedAddress;
	}

	@Override
	public int compareTo(Peer o) {

		long weight = getWeight(), weight2 = o.getWeight();
		if (weight > weight2) {

			return -1;

		} else if (weight < weight2) {

			return 1;

		} else {

			return index - o.index;

		}
	}

	public long getWeight() {

		// TODO not yet implemented
		return 0;

	}

	/**
	 * Returns the node state.
	 * 
	 * @return
	 */
	public State getState() {
		return state;
	}

	/**
	 * Sets the node state.
	 * 
	 * @param state
	 */
	public void setState(State state) {
		this.state = state;
	}

	/**
	 * Returns the time the node was added to the blacklist.
	 * 
	 * @return
	 */
	public long getBlacklistingTime() {
		return blacklistingTime;
	}

	/**
	 * Sets the time when remote node was added to the black list.
	 * 
	 * @param value
	 */
	public void setBlacklistingTime(long value) {
		this.blacklistingTime = value;
	}

	/**
	 * Returns the characteristics of the remote node for identification, search
	 * for provided services, etc.
	 * 
	 * @return
	 */
	public Metadata getMetadata() {
		return metadata;
	}

	/**
	 * Sets the characteristics of the remote node.
	 * 
	 * @param metadata
	 */
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

}
