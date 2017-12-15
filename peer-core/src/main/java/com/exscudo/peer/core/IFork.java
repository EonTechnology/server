package com.exscudo.peer.core;

import com.exscudo.peer.core.services.ITransactionHandler;

/**
 * Hard fork is a pre-planned network update point. At that point in time, new
 * functionality is introduced into the network.
 * <p>
 * If the fork is expired, the state of the node is considered as irrelevant. In
 * this case, the tasks of synchronization and the generation of new blocks must
 * be stopped.
 */
public interface IFork {

	/**
	 * Returns genesis-block ID.
	 * <p>
	 * Genesis block can be considered as a network identifier.
	 */
	long getGenesisBlockID();

	/**
	 * Checks whether the hard-fork has been expired for the specified
	 * {@code timestamp}.
	 *
	 * @param timestamp
	 *            for which a check is made (unix timestamp)
	 * @return true if time not in fork, otherwise false
	 */
	boolean isPassed(int timestamp);

	/**
	 * Checks whether the hard-fork started at the specified {@code timestamp}.
	 * <p>
	 * The moment that the fork was started at the specified time, does not
	 * guarantee that it was not already completed
	 *
	 * @param timestamp
	 *            for which a check is made (unix timestamp)
	 * @return true if the hard-fork was started, otherwise - false.
	 */
	boolean isCome(int timestamp);

	/**
	 * Returns hard-fork number.
	 * <p>
	 * Hard-forks are numbered sequentially in ascending order.
	 *
	 * @param timestamp
	 *            on which it is necessary to calculate the number of the hard-fork
	 *            (unix timestamp)
	 * @return hard-fork number
	 */
	int getNumber(int timestamp);

	/**
	 * Returns the transaction handler used for the specified time or null.
	 *
	 * @param timestamp
	 *            for which a handler will be returned (unix timestamp)
	 * @return transaction handler or null
	 */
	ITransactionHandler getTransactionExecutor(int timestamp);

	// TODO: change to IBlockHandler
	/**
	 * Returns the block version used for the specified time.
	 *
	 * @param timestamp
	 *            for which a block version will be returned (unix timestamp)
	 * @return block version or -1
	 */
	int getBlockVersion(int timestamp);

}
