package com.exscudo.peer.core.env;

import java.util.Random;

/**
 * The class describes an object associated with a remote env.
 * <p>
 * Used to storage the state and to describe configuration of a services env.
 */
public class PeerInfo implements Comparable<PeerInfo> {

    /**
     * The env has an unknown state. 1. The initial state of the env. 2. In this
     * state the env gets after exclusion from the black list.
     */
    public final static int STATE_AMBIGUOUS = 10;
    /**
     * The env is in the pool of active nodes (used to synchronize data)
     */
    public final static int STATE_CONNECTED = 20;
    /**
     * In this state, the env falls if it does not respond, or use unsupported
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
    final int index;
    final String address;
    private final int type;
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
     * Returns the state of the env relative to the network.
     *
     * @return true if the host address not be distributed over the network,
     * otherwise- false
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
     * Returns the env state.
     *
     * @return
     */
    public int getState() {
        return state;
    }

    /**
     * Sets the env state.
     *
     * @param state
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * Returns the time the env was added to the blacklist.
     *
     * @return
     */
    public long getBlacklistingTime() {
        return blacklistingTime;
    }

    /**
     * Sets the time when services env was added to the black list.
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
     * Returns the characteristics of the services env for identification, search
     * for provided services, etc.
     *
     * @return
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the characteristics of the services env.
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

        /**
         * A random number generated at env startup.
         */
        private final long peerID;
        /**
         * Application identifier.
         */
        private final String application;
        /**
         * Version of the env.
         */
        private final String version;

        public Metadata(long peerID, String application, String version) {

            this.peerID = peerID;
            this.application = application;
            this.version = version;
        }

        public long getPeerID() {
            return peerID;
        }

        public String getApplication() {
            return application;
        }

        public String getVersion() {
            return version;
        }
    }
}
