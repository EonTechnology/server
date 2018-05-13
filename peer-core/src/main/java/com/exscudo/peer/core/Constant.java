package com.exscudo.peer.core;

public class Constant {

    /**
     * Number of seconds in the day.
     */
    public static final int SECONDS_IN_DAY = 24 * 60 * 60;
    /**
     * The interval of a new block creation.
     */
    public static final int BLOCK_PERIOD = 180;
    /**
     * The number of blocks that are created per day.
     */
    public static final int BLOCK_IN_DAY = SECONDS_IN_DAY / BLOCK_PERIOD;
    /**
     * The block offset in the chain that is used to calculate the generation
     * signature.
     */
    public static final int DIFFICULTY_DELAY = BLOCK_IN_DAY;
    /**
     * The number of blocks, which are used for the long synchronization.
     */
    public static final int SYNC_LONG_FRAME = BLOCK_IN_DAY;
    /**
     * The maximum depth of the chain branching.
     */
    @SuppressWarnings("PointlessArithmeticExpression")
    public static final int SYNC_MILESTONE_DEPTH = BLOCK_IN_DAY / 1;
    /**
     * The time interval of a possible delay of a node.
     */
    public static final int MAX_LATENCY = 15;
    /**
     * The maximum size of the data in the block.
     */
    @SuppressWarnings("PointlessArithmeticExpression")
    public static final long BLOCK_MAX_PAYLOAD_LENGTH = 1 * 1024 * 1024;
    /**
     * Max transaction count in block
     */
    public static final int BLOCK_TRANSACTION_LIMIT = 4000;
    /**
     * Minimum fee by the transaction processing.
     */
    public static final double TRANSACTION_MIN_FEE_PER_BYTE = 10.0 / 1024;
    /**
     * Maximum transaction size in bytes
     */
    public static final int TRANSACTION_MAX_PAYLOAD_LENGTH = 1024 * 4;
    /**
     * Maximum allowed lifetime of a transaction.
     */
    public static final int TRANSACTION_MAX_LIFETIME = 600 * 60;
    /**
     * The maximum "Note" field size in a transaction
     */
    public static final int TRANSACTION_NOTE_MAX_LENGTH = 128;
    /**
     * Maximum allowed confirmations of a account.
     */
    public static final int TRANSACTION_CONFIRMATIONS_MAX_SIZE = 20;
    /**
     * The number of blocks, which are stored on limited blockchain
     */
    public static final int STORAGE_FRAME_BLOCK = BLOCK_IN_DAY * 7;
    /**
     * The number of blocks that are created per hour.
     */
    static final int BLOCK_IN_HOUR = (60 * 60) / BLOCK_PERIOD;
    /**
     * The number of blocks, which are used for the quick synchronization.
     */
    public static final int SYNC_SHORT_FRAME = BLOCK_IN_HOUR;
}
