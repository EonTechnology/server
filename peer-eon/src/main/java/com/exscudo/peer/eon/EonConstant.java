package com.exscudo.peer.eon;

/**
 * List of constants.
 */
public class EonConstant {

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
    public static final double TRANSACTION_MIN_FEE_PER_BYTE = 10.0 / 1024;

    /**
     * Maximum fee by the transaction processing.
     */
    public static final long TRANSACTION_MAX_FEE = MAX_MONEY;

    /**
     * Maximum transaction size in bytes
     */
    public static final int TRANSACTION_MAX_PAYLOAD_LENGTH = 1024;

    /**
     * Maximum allowed lifetime of a transaction.
     */
    public static final int TRANSACTION_MAX_LIFETIME = 600 * 60;

}
