package com.exscudo.peer.eon;

import java.util.Collection;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import com.exscudo.peer.core.AbstractContext;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.IPeer;

/**
 * Context within which tasks are performed.
 *
 */
public class ExecutionContext extends AbstractContext<Peer, Instance> {

	public ExecutionContext(TimeProvider time, IFork fork) {
		this.timeProvider = time;
		this.fork = fork;
	}

	/**
	 * Returns the version.
	 *
	 * @return
	 */
	public String getVersion() {
		return "0.8.0";
	}

	/**
	 * Returns the application name.
	 *
	 * @return
	 */
	public String getApplication() {
		return "EON";
	}

	/* The host on which the node is running */
	private Host host;

	/**
	 * Returns properties of the host on which the node is running.
	 *
	 * @return
	 */
	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}

	/* Time Provider */
	private TimeProvider timeProvider;

	public TimeProvider getTimeProvider() {
		return timeProvider;
	}

	@Override
	public int getCurrentTime() {
		return timeProvider.get();
	}

	/* Hard-fork */
	private IFork fork;

	@Override
	public IFork getCurrentFork() {
		return fork;
	}

	/**/
	private Instance peer;

	@Override
	public Instance getInstance() {
		return peer;
	}

	public void setPeer(Instance peer) {
		this.peer = peer;
	}

	/* Register of known peers */
	private final PeerRegistry peers = new PeerRegistry();

	public PeerRegistry getPeers() {
		return peers;
	}

	/* The time during which the node will be located in the black list. */
	private int blacklistingPeriod = 30000;

	/**
	 * Gets the time during which the node will be located in the black list.
	 *
	 * @return
	 */
	public int getBlacklistingPeriod() {
		return blacklistingPeriod;
	}

	public void setBlacklistingPeriod(int blacklistingPeriod) {
		this.blacklistingPeriod = blacklistingPeriod;
	}

	/* time that the node is in the list of connected */
	private int connectingPeriod = 60 * 1000;

	public int getConnectingPeriod() {
		return connectingPeriod;
	}

	public void setConnectingPeriod(int connectingPeriod) {
		this.connectingPeriod = connectingPeriod;
	}

	@Override
	public boolean disablePeer(IPeer peer) {

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

	@Override
	public boolean blacklistPeer(IPeer peer, long timestamp) {
		PeerInfo pi = getPeer(peer);
		if (pi == null) {
			return false;
		}
		pi.setBlacklistingTime(timestamp);
		return true;
	}

	public boolean connectPeer(IPeer peer) {
		return connectPeer(peer, getConnectingPeriod());
	}

	public boolean connectPeer(IPeer peer, int duration) {
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

	public PeerInfo getPeer(IPeer peer) {
		Objects.requireNonNull(peer);
		PeerInfo pi = peers.findFirst(new Predicate<PeerInfo>() {
			@Override
			public boolean test(PeerInfo pi) {
				return pi.getMetadata().getPeerID() == peer.getPeerID();
			}
		});

		return pi;
	}

	/* Factory to create a stub of the service */
	private IServiceProxyFactory proxyFactory;

	public IServiceProxyFactory getProxyFactory() {
		return proxyFactory;
	}

	public void setProxyFactory(IServiceProxyFactory proxyFactory) {
		this.proxyFactory = proxyFactory;
	}

	private boolean isInnerPeersUsing = false;

	/**
	 * Returns the size of the active node pool.
	 *
	 * @return
	 */
	public int getConnectedPoolSize() {

		int peerCount = peers.count(new Predicate<PeerInfo>() {

			@Override
			public boolean test(PeerInfo peer) {
				return peer.getState() == PeerInfo.STATE_CONNECTED && peer.getAddress().length() > 0;
			}
		});

		return peerCount;

	}

	/**
	 * Returns the active node randomly chosen.
	 *
	 * @return
	 */
	@Override
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
	 * Returns the node selected at random for connection.
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

	public static class Host {
		private static final Random random = new Random();

		public Host(String address) {
			this.address = address;
		}

		private final long peerID = random.nextLong();

		/**
		 * Returns identifier of the current node.
		 *
		 * @return
		 */
		public long getPeerID() {
			return peerID;
		}

		private final String address;

		/**
		 * Returns the node holder account number
		 *
		 * @return
		 */
		public String getAddress() {
			return address;
		}
	}

	abstract static class PeerBasePredicate implements Predicate<PeerInfo> {
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
			return peer.getBlacklistingTime() <= 0 && peer.getAddress().length() != 0
					&& peer.getMetadata().getPeerID() != hostPeerId && (peer.isInner() == peerShouldBeInner);
		}
	}

}
