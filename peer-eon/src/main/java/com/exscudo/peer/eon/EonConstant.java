package com.exscudo.peer.eon;

import com.exscudo.peer.core.Constant;

/**
 * List of constants.
 *
 */
public class EonConstant {

	/**
	 * The block offset in the chain that is used to calculate the generation
	 * signature.
	 */
	public static final int DIFFICULTY_DELAY = Constant.BLOCK_IN_DAY;

	/**
	 * Decimal points in 1 EON
	 */
	public static final long DECIMAL_POINT = 1000000L;

	/**
	 * Total number of coins.
	 */
	public static final long MAX_MONEY = 240000000L * DECIMAL_POINT;

	/**
	 * Determines the minimal amount of deposit witch required to participate in the
	 * generation of blocks.
	 */
	public static final long MIN_DEPOSIT_SIZE = 500L * DECIMAL_POINT;

	/**
	 * Minimum fee by the transaction processing.
	 */
	public static final long TRANSACTION_MIN_FEE = 1;

	/**
	 * Maximum fee by the transaction processing.
	 */
	public static final long TRANSACTION_MAX_FEE = MAX_MONEY;

	/**
	 * Maximum transaction size in bytes
	 */
	public static final long TRANSACTION_MAX_PAYLOAD_LENGTH = 1024L;

	/**
	 * Maximum allowed lifetime of a transaction.
	 */
	public static final short TRANSACTION_MAX_LIFETIME = 600;

	/**
	 * The maximum size of the data in the block.
	 */
	public static final long BLOCK_MAX_PAYLOAD_LENGTH = 1 * 1024 * 1024;

	/**
	 * Max transaction count in block
	 */
	public static final int BLOCK_TRANSACTION_LIMIT = 4000;

}
