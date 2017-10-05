package com.exscudo.peer.core;

import java.util.Objects;

import com.exscudo.peer.core.events.DispatchableEvent;
import com.exscudo.peer.core.events.Dispatcher;
import com.exscudo.peer.core.events.IPeerEventListener;
import com.exscudo.peer.core.events.PeerEvent;

/**
 * Class {@code AbstractContext} provides a basic implementation of the context
 * within which tasks are performed.
 * <p>
 * A task is a certain action that is performed periodically, the execution of
 * which leads to a change in the current state. It is through the context, task
 * are given access to the environment variables and the set of services through
 * which data is accessed.
 * <p>
 * 
 * @param <TPeer>
 *            The type of an object that provides access to the set of services
 *            provided by the remote peer
 * 
 * @param <TInstance>
 *            The type of an object that provides access to a set of services
 *            for accessing data on the current node
 */
public abstract class AbstractContext<TPeer extends IPeer, TInstance extends IInstance> {
	private final PeerEventListenerSupport peerEventSupport = new PeerEventListenerSupport();

	/**
	 * Returns current time in seconds (unix timestamp)
	 * 
	 * @return current time
	 */
	public abstract int getCurrentTime();

	/**
	 * Returns current fork
	 *
	 * @return current fork
	 */
	public Fork getCurrentFork() {
		return ForkProvider.getInstance();
	}

	/**
	 * Checks the status of the current hard-fork.
	 * 
	 * @return true - if current hard-fork in progress, otherwise - false
	 */
	public boolean isCurrentForkPassed() {
		return getCurrentFork().isPassed(getCurrentTime());
	}

	/**
	 * Checks the status of the current hard-fork at the specified {@code timestamp}
	 * 
	 * @param timestamp
	 *            in which it is necessary to check the condition of hard fork.
	 * @return true - if current hard-fork in progress, otherwise - false
	 */
	public boolean isCurrentForkPassed(int timestamp) {
		return getCurrentFork().isPassed(timestamp);
	}

	/**
	 * Returns instance of an object that provides access to the set of services for
	 * accessing data.
	 * 
	 * @return instance
	 */
	public abstract TInstance getInstance();

	/**
	 * Returns instance of an object that provides access to the set of remote
	 * services for a randomly selected node.
	 * 
	 * @return instance of an object or null
	 */
	public abstract TPeer getAnyConnectedPeer();

	/**
	 * Change passed {@code peer} to disconnected state.
	 * 
	 * @param peer
	 *            that must be disabled. Can not be equal to null.
	 * @return true if peer exist, otherwise - false
	 * @throws NullPointerException
	 *             if {@code peer} is null
	 */
	public abstract boolean disablePeer(IPeer peer);

	/**
	 * Puts specified {@code peer} to blacklist.
	 * 
	 * @param peer
	 *            that must be blacklisted. Can not be equal to null.
	 * @param timestamp
	 *            by which the node should be added to the black list
	 * @return true if peer exist, otherwise - false
	 * @throws NullPointerException
	 *             if {@code peer} is null
	 */
	public abstract boolean blacklistPeer(IPeer peer, long timestamp);

	/**
	 * Puts specified {@code peer} to blacklist.
	 * 
	 * @param peer
	 *            that must be blacklisted. Can not be equal to null.
	 * @return true if peer exist, otherwise - false
	 * @throws NullPointerException
	 *             if {@code peer} is null
	 */
	public boolean blacklistPeer(IPeer peer) {
		return blacklistPeer(peer, System.currentTimeMillis());
	}

	/**
	 * Adds listener.
	 * 
	 * @param listener
	 *            to add to the list
	 */
	public void addListener(IPeerEventListener listener) {
		Objects.requireNonNull(listener);

		peerEventSupport.addListener(listener);
	}

	/**
	 * Removes listener.
	 * 
	 * @param listener
	 *            to remove from the list
	 */
	public void removeListener(IPeerEventListener listener) {
		Objects.requireNonNull(listener);

		peerEventSupport.removeListener(listener);
	}

	/**
	 * Initiate an event that indicates that the chain of blocks state is the same
	 * as the specified remote node.
	 * 
	 * @param source
	 *            that reports an occurrence of an event. Can not be null
	 * @param peer
	 *            from which the chain of the blocks was matched
	 */
	public void raiseSynchronizedEvent(Object source, IPeer peer) {
		peerEventSupport.raiseEvent(new DispatchableEvent<IPeerEventListener, PeerEvent>(new PeerEvent(source, peer)) {
			@Override
			public void dispatch(IPeerEventListener target, PeerEvent event) {
				target.onSynchronized(event);
			}
		});
	}

	private static class PeerEventListenerSupport extends Dispatcher<IPeerEventListener> {
		PeerEventListenerSupport() {
			super();
		}
	}

}
