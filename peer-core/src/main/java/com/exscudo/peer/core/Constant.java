package com.exscudo.peer.core;

public class Constant {

	/**
	 * The interval of a new block creation.
	 */
	public static final int BLOCK_PERIOD = 180;

	/**
	 * The number of blocks that are created per hour.
	 */
	static final int BLOCK_IN_HOUR = (60 * 60) / BLOCK_PERIOD;

	/**
	 * The number of blocks that are created per day.
	 */
	public static final int BLOCK_IN_DAY = (24 * 60 * 60) / BLOCK_PERIOD;

	/**
	 * The number of blocks, which are used for the quick synchronization.
	 */
	public static final int SYNC_SHORT_FRAME = BLOCK_IN_HOUR;

	/**
	 * The number of blocks, which are used for the long synchronization.
	 */
	public static final int SYNC_LONG_FRAME = BLOCK_IN_DAY;

	/**
	 * The maximum depth of the chain branching.
	 */
	public static final int SYNC_MILESTONE_DEPTH = BLOCK_IN_DAY / 1;

	/**
	 * The time interval of a possible delay of a node.
	 */
	public static final int MAX_LATENCY = 15;

}
