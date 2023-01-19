package org.eontechnology.and.peer.core;

public class Constant {

  /** Number of seconds in the day. */
  public static final int SECONDS_IN_DAY = 24 * 60 * 60;
  /** The block offset in the chain that is used to calculate the generation signature. */
  public static final int DIFFICULTY_DELAY_SECONDS = SECONDS_IN_DAY;
  /** The time interval of a possible delay of a node. */
  public static final int MAX_LATENCY = 15;
  /** Max transaction count in block */
  public static final int BLOCK_TRANSACTION_LIMIT = 4000;
  /** Minimum fee by the transaction processing. */
  public static final double TRANSACTION_MIN_FEE_PER_BYTE = 10.0 / 1024;
  /** Maximum transaction size in bytes */
  public static final int TRANSACTION_MAX_PAYLOAD_LENGTH = 1024 * 4;
  /** Maximum allowed lifetime of a transaction. */
  public static final int TRANSACTION_MAX_LIFETIME = 600 * 60;
  /** The maximum "Note" field size in a transaction */
  public static final int TRANSACTION_NOTE_MAX_LENGTH = 128;

  public static final int TRANSACTION_NOTE_MAX_LENGTH_V2 = 256;
  /** Maximum allowed confirmations of a account. */
  public static final int TRANSACTION_CONFIRMATIONS_MAX_SIZE = 20;
  /** The block age, which are stored on limited blockchain */
  public static final int STORAGE_FRAME_AGE = SECONDS_IN_DAY * 7;
  /** The number of blocks, which are used for the quick synchronization. */
  public static final int SYNC_SHORT_FRAME = 20;
}
