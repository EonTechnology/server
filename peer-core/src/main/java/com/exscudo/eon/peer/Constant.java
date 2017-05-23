package com.exscudo.eon.peer;

public class Constant {

	/**
	 * The interval over which to create new block.
	 */
	public static final int BLOCK_PERIOD = 180;

	/**
	 * The number of blocks that are created per hour.
	 */
	static final int BLOCK_IN_HOUR = (60 * 60) / BLOCK_PERIOD;

	/**
	 * The number of blocks that are created per day.
	 */
	static final int BLOCK_IN_DAY = (24 * 60 * 60) / BLOCK_PERIOD;

	/**
	 * The number of blocks, which is used to quick synchronization.
	 */
	public static final int SYNC_SHORT_FRAME = BLOCK_IN_HOUR;

	/**
	 * The number of blocks, which is used to long synchronization.
	 */
	public static final int SYNC_LONG_FRAME = BLOCK_IN_DAY;

	/**
	 * The maximum depth of the chain branching.
	 */
	public static final int SYNC_MILESTONE_DEPTH = BLOCK_IN_DAY / 1;

	/**
	 * The time interval at which a node may be a delay.
	 */
	public static final int MAX_LATENCY = 15;

	/**
	 * Maximum allowed lifetime of a transaction.
	 */
	public static final short MAX_TX_LIFETIME = 600;

	/**
	 * The maximum number of transactions in a packet when synchronizing
	 * unconfirmed transactions.
	 */
	public static final int TRANSACTION_SIZE_LIMIT = 100;

	// ATTENTION.
	// TODO: Recalculate taking into account the economic model.
	public static final long TX_MAX_PAYLOAD_LENGTH = 1048576L;

	// ATTENTION.
	// TODO: Recalculate taking into account the economic model.
	public static final long BLOCK_MAX_PAYLOAD_LENGTH = 1024 * 1024;

}
