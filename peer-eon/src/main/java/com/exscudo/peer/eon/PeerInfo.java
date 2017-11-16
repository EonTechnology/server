package com.exscudo.peer.eon;

import java.util.Random;

/**
 * The class describes an object associated with a remote node.
 * <p>
 * Used to storage the state and to describe configuration of a services node.
 *
 */
public class PeerInfo implements Comparable<PeerInfo> {

	/**
	 * The node has an unknown state. 1. The initial state of the node. 2. In this
	 * state the node gets after exclusion from the black list.
	 */
	public final static int STATE_AMBIGUOUS = 10;
	/**
	 * The node is in the pool of active nodes (used to synchronize data)
	 */
	public final static int STATE_CONNECTED = 20;
	/**
	 * In this state, the node falls if it does not respond, or use unsupported
	 * protocol.
	 */
	public final static int STATE_DISCONNECTED = 30;

	/**
	 * Peer from init list
	 */
	public final static int TYPE_IMMUTABLE = 10;
	/**
	 * Peer added from other peer
	 */
	public final static int TYPE_NORMAL = 20;
	/**
	 * Inner peer
	 */
	public final static int TYPE_INNER = 30;

	private static final Random random = new Random();

	private final int type;
	final int index;
	final String address;
	private volatile int state;
	private volatile long blacklistingTime;
	private volatile long connectingTime;
	private volatile Metadata metadata;

	public PeerInfo(int index, String address, int type) {

		this.address = address;
		this.index = index;
		this.type = type;

		state = STATE_AMBIGUOUS;

		metadata = new Metadata(random.nextLong(), null, null);

	}

	/**
	 * Returns the state of the node relative to the network.
	 * 
	 * @return true if the host address not be distributed over the network,
	 *         otherwise- false
	 */
	public boolean isInner() {
		return type == TYPE_INNER;
	}

	public boolean isImmutable() {
		return type == TYPE_IMMUTABLE;
	}

	public String getAddress() {
		return address;
	}

	@Override
	public int compareTo(PeerInfo o) {
		return Integer.compare(index, o.index);
	}

	/**
	 * Returns the node state.
	 *
	 * @return
	 */
	public int getState() {
		return state;
	}

	/**
	 * Sets the node state.
	 *
	 * @param state
	 */
	public void setState(int state) {
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
	 * Sets the time when services node was added to the black list.
	 *
	 * @param value
	 */
	public void setBlacklistingTime(long value) {
		this.blacklistingTime = value;
	}

	/**
	 * Returns the time when the services host will be disconnected
	 *
	 * @return
	 */
	public long getConnectingTime() {
		return connectingTime;
	}

	/**
	 * Sets the time when the services host will be disconnected
	 *
	 * @param connectingTime
	 */
	public void setConnectingTime(long connectingTime) {
		this.connectingTime = connectingTime;
	}

	/**
	 * Returns the characteristics of the services node for identification, search
	 * for provided services, etc.
	 *
	 * @return
	 */
	public Metadata getMetadata() {
		return metadata;
	}

	/**
	 * Sets the characteristics of the services node.
	 *
	 * @param metadata
	 */
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	//
	// The configuration of a services host.
	//
	public static class Metadata {

		public long getPeerID() {
			return peerID;
		}

		public String getApplication() {
			return application;
		}

		public String getVersion() {
			return version;
		}

		/**
		 * A random number generated at node startup.
		 */
		private final long peerID;

		/**
		 * Application identifier.
		 */
		private final String application;

		/**
		 * Version of the node.
		 */
		private final String version;

		public Metadata(long peerID, String application, String version) {

			this.peerID = peerID;
			this.application = application;
			this.version = version;

		}
	}

}
