package com.exscudo.peer.core;

/**
 * Represents a provider of current hard-fork.
 */
public class ForkProvider {

	private static Fork instance;

	/**
	 * Returns current hard-fork.
	 * <p>
	 * If the fork is expired, the state of the node is considered as irrelevant. In
	 * this case, the tasks of synchronization and the generation of new blocks must
	 * be stopped.
	 *
	 * @return current hard-fork
	 */
	public static Fork getInstance() {
		return instance;
	}

	/**
	 * Set passed hard-fork as current.
	 *
	 * @param fork
	 *            to set as current.
	 */
	public static void init(Fork fork) {
		instance = fork;
	}
}
