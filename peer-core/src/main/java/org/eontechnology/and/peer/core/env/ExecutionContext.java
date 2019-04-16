package org.eontechnology.and.peer.core.env;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eontechnology.and.peer.core.common.events.DispatchableEvent;
import org.eontechnology.and.peer.core.common.events.Dispatcher;
import org.eontechnology.and.peer.core.env.events.IPeerEventListener;
import org.eontechnology.and.peer.core.env.events.PeerEvent;

/**
 * Context within which tasks are performed.
 */
public class ExecutionContext {

    private final PeerEventListenerSupport peerEventSupport = new PeerEventListenerSupport();

    /* Register of known peers */
    private final PeerRegistry peers = new PeerRegistry();

    /* The host on which the env is running */
    private Host host;

    /* The time during which the env will be located in the black list. */
    private int blacklistingPeriod = 30000;

    /* time that the env is in the list of connected */
    private int connectingPeriod = 3 * 60 * 1000;

    /* pool size for connected peers */
    private int connectedPoolSize = 25;

    /* Factory to create a stub of the service */
    private IServiceProxyFactory proxyFactory;

    private boolean isInnerPeersUsing = false;

    private String version = "";
    private String application = "";

    /**
     * Returns the version.
     *
     * @return
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the application name.
     *
     * @return
     */
    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    /**
     * Returns properties of the host on which the env is running.
     *
     * @return
     */
    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    /**
     * Returns the active env randomly chosen.
     *
     * @return
     */
    public Peer getAnyConnectedPeer() {

        PeerInfo pi = getAnyPeer(new PeerBasePredicate(getHost().getPeerID(), isInnerPeersUsing) {

            @Override
            public boolean test(PeerInfo peer) {
                return super.test(peer) && peer.getState() == PeerInfo.STATE_CONNECTED;
            }
        });
        if (pi == null) {
            return null;
        }

        return new Peer(pi, getProxyFactory());
    }

    /**
     * Returns the active peers randomly chosen.
     *
     * @return
     */
    public List<Peer> getAnyConnectedPeers(int count) {
        List<PeerInfo> list = getAnyPeers(new PeerBasePredicate(getHost().getPeerID(), isInnerPeersUsing) {

            @Override
            public boolean test(PeerInfo peer) {
                return super.test(peer) && peer.getState() == PeerInfo.STATE_CONNECTED;
            }
        }, count);

        if (list == null) {
            return null;
        }

        return list.stream().map(new Function<PeerInfo, Peer>() {
            @Override
            public Peer apply(PeerInfo peerInfo) {
                return new Peer(peerInfo, getProxyFactory());
            }
        }).collect(Collectors.toList());
    }

    /**
     * Change passed {@code peer} to disconnected state.
     *
     * @param peer that must be disabled. Can not be equal to null.
     * @return true if peer exist, otherwise - false
     * @throws NullPointerException if {@code peer} is null
     */
    public boolean disablePeer(Peer peer) {

        PeerInfo pi = getPeer(peer);
        if (pi == null) {
            return false;
        }
        synchronized (pi) {
            if (pi.getState() == PeerInfo.STATE_AMBIGUOUS) {
                pi.setBlacklistingTime(System.currentTimeMillis());
            } else {
                pi.setState(PeerInfo.STATE_DISCONNECTED);
            }
        }
        return true;
    }

    /**
     * Puts specified {@code peer} to blacklist.
     *
     * @param peer      that must be blacklisted. Can not be equal to null.
     * @param timestamp by which the env should be added to the black list
     * @return true if peer exist, otherwise - false
     * @throws NullPointerException if {@code peer} is null
     */
    public boolean blacklistPeer(Peer peer, long timestamp) {
        PeerInfo pi = getPeer(peer);
        if (pi == null) {
            return false;
        }
        pi.setBlacklistingTime(timestamp);
        return true;
    }

    /**
     * Puts specified {@code peer} to blacklist.
     *
     * @param peer that must be blacklisted. Can not be equal to null.
     * @return true if peer exist, otherwise - false
     * @throws NullPointerException if {@code peer} is null
     */
    public boolean blacklistPeer(Peer peer) {
        return blacklistPeer(peer, System.currentTimeMillis());
    }

    /**
     * Adds listener.
     *
     * @param listener to add to the list
     */
    public void addListener(IPeerEventListener listener) {
        Objects.requireNonNull(listener);

        peerEventSupport.addListener(listener);
    }

    /**
     * Removes listener.
     *
     * @param listener to remove from the list
     */
    public void removeListener(IPeerEventListener listener) {
        Objects.requireNonNull(listener);

        peerEventSupport.removeListener(listener);
    }

    /**
     * Initiate an event that indicates that the chain of blocks state is the same
     * as the specified remote env.
     *
     * @param source that reports an occurrence of an event. Can not be null
     * @param peer   from which the chain of the blocks was matched
     */
    public void raiseSynchronizedEvent(Object source, Peer peer) {
        peerEventSupport.raiseEvent(new DispatchableEvent<IPeerEventListener, PeerEvent>(new PeerEvent(source, peer)) {
            @Override
            public void dispatch(IPeerEventListener target, PeerEvent event) {
                target.onSynchronized(event);
            }
        });
    }

    public PeerRegistry getPeers() {
        return peers;
    }

    /**
     * Gets the time during which the env will be located in the black list.
     *
     * @return
     */
    public int getBlacklistingPeriod() {
        return blacklistingPeriod;
    }

    public void setBlacklistingPeriod(int blacklistingPeriod) {
        this.blacklistingPeriod = blacklistingPeriod;
    }

    public int getConnectingPeriod() {
        return connectingPeriod;
    }

    public void setConnectingPeriod(int connectingPeriod) {
        this.connectingPeriod = connectingPeriod;
    }

    public int getConnectedPoolSize() {
        return connectedPoolSize;
    }

    public void setConnectedPoolSize(int connectedPoolSize) {
        this.connectedPoolSize = connectedPoolSize;
    }

    public boolean connectPeer(Peer peer) {
        return connectPeer(peer, getConnectingPeriod());
    }

    public boolean connectPeer(Peer peer, int duration) {
        PeerInfo pi = getPeer(peer);
        if (pi == null) {
            return false;
        }

        synchronized (pi) {
            pi.setState(PeerInfo.STATE_CONNECTED);
            pi.setConnectingTime(System.currentTimeMillis() + duration);
        }
        return true;
    }

    public PeerInfo getPeer(Peer peer) {
        Objects.requireNonNull(peer);
        PeerInfo pi = peers.findFirst(new Predicate<PeerInfo>() {
            @Override
            public boolean test(PeerInfo pi) {
                return pi.getMetadata().getPeerID() == peer.getPeerID();
            }
        });

        return pi;
    }

    public IServiceProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    public void setProxyFactory(IServiceProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    /**
     * Returns the size of the active env pool.
     *
     * @return
     */
    public int getConnectedPeerCount() {

        int peerCount = peers.count(new Predicate<PeerInfo>() {

            @Override
            public boolean test(PeerInfo peer) {
                return peer.getState() == PeerInfo.STATE_CONNECTED && peer.getAddress().length() > 0;
            }
        });

        return peerCount;
    }

    /**
     * Returns the env selected at random for connection.
     *
     * @return
     */
    public Peer getAnyPeerToConnect() {

        PeerInfo pi = getAnyPeer(new PeerBasePredicate(getHost().getPeerID(), isInnerPeersUsing) {
            @Override
            public boolean test(PeerInfo peer) {
                return super.test(peer) && peer.getState() != PeerInfo.STATE_CONNECTED;
            }
        });

        if (pi == null) {
            return null;
        }
        return new Peer(pi, getProxyFactory());
    }

    public Peer getAnyDisabledPeer() {
        final PeerInfo pi = getAnyPeer(new Predicate<PeerInfo>() {
            @Override
            public boolean test(PeerInfo peer) {
                return peer.getState() != PeerInfo.STATE_CONNECTED || peer.getBlacklistingTime() > 0;
            }
        });

        if (pi == null) {
            return null;
        }

        return new Peer(pi, getProxyFactory());
    }

    public PeerInfo getAnyPeer(Predicate<PeerInfo> predicate) {

        try {

            Collection<PeerInfo> clone = peers.findAll(predicate);
            if (clone.size() > 0) {

                PeerInfo[] selectedPeers = clone.toArray(new PeerInfo[0]);
                int hit = ThreadLocalRandom.current().nextInt(selectedPeers.length);
                return selectedPeers[hit];
            }

            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<PeerInfo> getAnyPeers(Predicate<PeerInfo> predicate, int count) {
        try {

            List<PeerInfo> clone = peers.findAll(predicate);
            if (clone.size() > 0) {

                if (count > clone.size()) {
                    return clone;
                }

                List<PeerInfo> selected = new LinkedList<>();
                while (selected.size() != count) {

                    int hit = ThreadLocalRandom.current().nextInt(clone.size());
                    PeerInfo pi = clone.remove(hit);
                    selected.add(pi);
                }
                return selected;
            }

            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addPublicPeer(String address) {

        if (address.length() > 0) {
            peers.addPeer(address, PeerInfo.TYPE_NORMAL);
        }
    }

    public void addInnerPeer(String address) {

        if (address.length() > 0) {
            peers.addPeer(address, PeerInfo.TYPE_INNER);
            isInnerPeersUsing = true;
        }
    }

    public void addImmutablePeer(String address) {

        if (address.length() > 0) {
            peers.addPeer(address, PeerInfo.TYPE_IMMUTABLE);
        }
    }

    private static class PeerEventListenerSupport extends Dispatcher<IPeerEventListener> {
        PeerEventListenerSupport() {
            super();
        }
    }

    public static class Host {
        private static final Random random = new Random();
        private final long peerID = random.nextLong();
        private final String address;

        public Host(String address) {
            this.address = address;
        }

        /**
         * Returns identifier of the current env.
         *
         * @return
         */
        public long getPeerID() {
            return peerID;
        }

        /**
         * Returns the env holder account number
         *
         * @return
         */
        public String getAddress() {
            return address;
        }
    }

    public abstract static class PeerBasePredicate implements Predicate<PeerInfo> {
        private final long hostPeerId;
        private boolean peerShouldBeInner = false;

        public PeerBasePredicate(long hostPeerId, boolean isInnerPeersUsing) {
            this.hostPeerId = hostPeerId;
            if (isInnerPeersUsing) {
                peerShouldBeInner = ThreadLocalRandom.current().nextBoolean();
            }
        }

        @Override
        public boolean test(PeerInfo peer) {
            return peer.getBlacklistingTime() <= 0 &&
                    peer.getAddress().length() != 0 &&
                    peer.getMetadata().getPeerID() != hostPeerId &&
                    (peer.isInner() == peerShouldBeInner);
        }
    }
}
