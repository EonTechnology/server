package com.exscudo.peer.eon;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

/**
 * Registry of all peers, known to the current runtime.
 *
 */
public class PeerRegistry {
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private final ConcurrentHashMap<String, PeerInfo> peers = new ConcurrentHashMap<String, PeerInfo>();
	private final ConcurrentHashMap<String, String> peersIP = new ConcurrentHashMap<String, String>();
	private volatile int peerCounter;

	/**
	 * Registers an unknown node and returns object associated with it. If the node
	 * with the specified {@code address} is already registered, the object
	 * associated with it will be returned.
	 * 
	 * @param address
	 *            node for adding. can not be null.
	 * @return object associated with passed {@code address}
	 */
	public PeerInfo addPeer(String address) {
		return addPeer(address, false);
	}

	/**
	 * Registers an unknown node and returns object associated with it. If the node
	 * with the specified {@code address} is already registered, the object
	 * associated with it will be returned.
	 * 
	 * @param address
	 *            node for adding. can not be null.
	 * @param inner
	 *            if true node is internal (the host address must not be distributed
	 *            over the network), Otherwise - false
	 * @return object associated with passed {@code address}
	 */
	public PeerInfo addPeer(String address, boolean inner) {
		Objects.requireNonNull(address);

		String ipAddress = address.split(":")[0];

		Lock lock = readWriteLock.writeLock();
		try {
			lock.lock();

			PeerInfo peer = peers.get(address);
			if (peer == null) {

				peer = new PeerInfo(++peerCounter, address, inner);
				peers.put(address, peer);

				peersIP.put(ipAddress, address);

			}
			return peer;

		} finally {
			lock.unlock();
		}

	}

	/**
	 * Returns an object associated with the node which has the specified
	 * {@code address} or null if the address is not exist.
	 * 
	 * @param address
	 *            node for searching. can not be null
	 * @return object associated with the node or null if the address is not exist.
	 */
	public PeerInfo getPeerByAddress(String address) {
		Objects.requireNonNull(address);
		return peers.get(address);
	}

	/**
	 * Returns an object associated with the node which has the specified {@code ip}
	 * or null if the address is not exist.
	 * 
	 * @param ip
	 *            for searching. can not be null
	 * @return object associated with the node or null if the ip is not exist.
	 */
	public PeerInfo getPeerByIP(String ip) {

		PeerInfo p = peers.get(ip);
		if (p == null && peersIP.containsKey(ip)) {
			p = peers.get(peersIP.get(ip));
		}

		if (p == null && peersIP.containsKey(ip)) {
			peersIP.remove(ip);
		}

		return p;
	}

	/**
	 * Returns a list of well-known peers.
	 *
	 * @return
	 */
	public String[] getPeersList() {

		return peers.keySet().toArray(new String[0]);
	}

	Collection<PeerInfo> findAll(Predicate<PeerInfo> predicate) {

		Collection<PeerInfo> collection = new LinkedList<PeerInfo>();
		Lock lock = readWriteLock.readLock();
		try {
			lock.lock();

			for (PeerInfo peer : peers.values()) {
				if (predicate.test(peer)) {
					collection.add(peer);
				}
			}

		} finally {
			lock.unlock();
		}

		return collection;
	}

	PeerInfo findFirst(Predicate<PeerInfo> predicate) {

		Lock lock = readWriteLock.readLock();
		try {
			lock.lock();

			for (PeerInfo peer : peers.values()) {
				if (predicate.test(peer)) {
					return peer;
				}
			}

			return null;
		} finally {
			lock.unlock();
		}

	}

	int count(Predicate<PeerInfo> predicate) {

		int numberOfConnectedPeers = 0;

		Lock lock = readWriteLock.readLock();
		try {
			lock.lock();

			for (PeerInfo peer : peers.values()) {
				if (predicate.test(peer)) {
					numberOfConnectedPeers++;
				}
			}

		} finally {
			lock.unlock();
		}

		return numberOfConnectedPeers;

	}

}
