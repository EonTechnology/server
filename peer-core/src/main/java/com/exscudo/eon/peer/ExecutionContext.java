package com.exscudo.eon.peer;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import javax.naming.NamingException;

import com.exscudo.eon.peer.Peer.State;

public abstract class ExecutionContext {

	/**
	 * Returns the version.
	 * 
	 * @return
	 */
	public abstract String getVersion();

	public static class Host {
		private static final Random random = new Random();
		
		public Host(String address, String hallmark) {
			this.address = address;
			this.hallmark = hallmark;
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

		private final String hallmark;

		public String getHallmark() {
			return hallmark;
		}

	}

	/**
	 * Returns properties of the host on which the node is running.
	 * 
	 * @return
	 */
	public abstract Host getHost();

	/**
	 * The state object supplied when the tasks was created. Used to synchronize
	 * running tasks.
	 */
	public abstract Object state();
	
	//
	// Peer Registry
	//

	public final PeerRegistry peers = new PeerRegistry();

	/**
	 * Returns the active node randomly chosen.
	 * 
	 * @return
	 */
	public Peer getAnyConnectedPeer() {

		return getAnyPeer(new Predicate<Peer>() {

			@Override
			public boolean test(Peer peer) {

				if (peer.getState() != State.STATE_CONNECTED
						|| (isEnableHallmarkProtection() && peer.getWeight() < getThreshold())) {
					return false;
				}

				if (peer.getBlacklistingTime() > 0 || peer.getAnnouncedAddress().length() == 0
						|| peer.getMetadata().peerID == getHost().getPeerID()) {
					return false;
				}

				return true;
			}
		});

	}

	/**
	 * Returns the size of the active node pool.
	 * 
	 * @return
	 */
	public int getConnectedPoolSize() {

		int peerCount = peers.count(new Predicate<Peer>() {

			@Override
			public boolean test(Peer peer) {
				return peer.getState() == State.STATE_CONNECTED && peer.getAnnouncedAddress().length() > 0;
			}
		});

		return peerCount;

	}

	/**
	 * Returns the maximum number of peers with which maintained connection.
	 * 
	 * @return
	 */
	public abstract int getMaxNumberOfConnectedPeers();

	/**
	 * Returns the node selected at random for connection.
	 * 
	 * @return
	 */
	public Peer getAnyPeerToConnect() {

		State state = (ThreadLocalRandom.current().nextInt(2) == 0 ? State.STATE_AMBIGUOUS : State.STATE_DISCONNECTED);

		return getAnyPeer(new Predicate<Peer>() {

			@Override
			public boolean test(Peer peer) {
				if (peer.getState() != state) {
					return false;
				}

				if (peer.getBlacklistingTime() > 0 || peer.getAnnouncedAddress().length() == 0
						|| peer.getMetadata().peerID == getHost().getPeerID()) {
					return false;
				}

				return true;
			}

		});
	}

	/**
	 * true if hallmark protection is enabled.
	 *
	 * @return
	 */
	private boolean isEnableHallmarkProtection() {

		// TODO: not yet implemented
		return false;
	}

	private int getThreshold() {

		// TODO: not yet implemented
		return 0;
	}

	private Peer getAnyPeer(Predicate<Peer> predicate) {

		try {

			Collection<Peer> clone = peers.findAll(predicate);
			if (clone.size() > 0) {

				Peer[] selectedPeers = clone.toArray(new Peer[0]);

				long totalWeight = 0;
				for (int i = 0; i < selectedPeers.length; i++) {

					long weight = selectedPeers[i].getWeight();
					if (weight == 0) {
						weight = 1;
					}
					totalWeight += weight;
				}

				long hit = ThreadLocalRandom.current().nextLong(totalWeight);
				for (int i = 0; i < selectedPeers.length; i++) {

					Peer peer = selectedPeers[i];
					long weight = peer.getWeight();
					if (weight == 0) {
						weight = 1;
					}

					if ((hit -= weight) < 0) {
						return peer;
					}
				}
			}

			return null;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the time during which the node will be located in the black list.
	 *
	 * @return
	 */
	public abstract int getBlacklistingPeriod();

	/**
	 * Create a proxy to the service.
	 * 
	 * @param peer
	 * @param clazz
	 * @return
	 * @throws NamingException
	 */
	public abstract <TService> TService createProxy(Peer peer, Class<TService> clazz);

	

}
