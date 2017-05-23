package com.exscudo.eon.peer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
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

	private final ConcurrentHashMap<String, Peer> peers = new ConcurrentHashMap<String, Peer>();
	private final ConcurrentHashMap<String, String> peersIP = new ConcurrentHashMap<String, String>();
	private volatile int peerCounter;

	/**
	 * Registers an unknown node and returns object associated with it. If the
	 * node with the specified <code>address</code> is already registered, the
	 * object associated with it will be returned.
	 * 
	 * @param address
	 * @param announcedAddress
	 * @return
	 */
	public Peer addPeer(String address, String announcedAddress) {
		return addPeer(address, announcedAddress, false);
	}

	public Peer addPeer(String address, String announcedAddress, boolean inner) {

		String useAddress = announcedAddress.length() > 0 ? announcedAddress : address;
		if (useAddress == null) {
			throw new IllegalArgumentException();
		}
		String ipAddress = useAddress.split(":")[0];

		Lock lock = readWriteLock.writeLock();
		try {
			lock.lock();

			Peer peer = peers.get(useAddress);
			if (peer == null) {

				peer = new Peer(++peerCounter, announcedAddress, inner);
				peers.put(useAddress, peer);
				
				peersIP.put(ipAddress, useAddress);

			}
			return peer;

		} finally {
			lock.unlock();
		}

	}

	public void removePeer(Peer peer) {

		Lock lock = readWriteLock.writeLock();
		try {
			lock.lock();

			for (Map.Entry<String, Peer> peerEntry : peers.entrySet()) {

				if (peerEntry.getValue() == peer) {

					peers.remove(peerEntry.getKey());
					break;
				}
			}
		} finally {
			lock.unlock();
		}

	}

	/**
	 * Returns an object associated with the node which has the specified
	 * <code>address</code> or null if the address is not exist.
	 * 
	 * @param address
	 * @return
	 * @throws NullPointerException
	 */
	public Peer getPeerByAddress(String address) {

		return peers.get(address);
	}

	/**
	 * Returns an object associated with the node which has the specified
	 * <code>ip</code> or null if the address is not exist.
	 * 
	 * @param ip
	 * @return
	 */
	public Peer getPeerByIP(String ip) {
		
		// TODO: need review
		
		Peer p = peers.get(ip);
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

	Collection<Peer> findAll(Predicate<Peer> predicate) {

		Collection<Peer> collection = new LinkedList<Peer>();
		Lock lock = readWriteLock.readLock();
		try {
			lock.lock();

			for (Peer peer : peers.values()) {
				if (predicate.test(peer)) {
					collection.add(peer);
				}
			}

		} finally {
			lock.unlock();
		}

		return collection;
	}

	int count(Predicate<Peer> predicate) {

		int numberOfConnectedPeers = 0;

		Lock lock = readWriteLock.readLock();
		try {
			lock.lock();

			for (Peer peer : peers.values()) {
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
