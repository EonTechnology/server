package org.eontechnology.and.peer.core;

import java.util.Set;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;

/**
 * Hard fork is a pre-planned network update point. At that point in time, new functionality is
 * introduced into the network.
 *
 * <p>If the fork is expired, the state of the env is considered as irrelevant. In this case, the
 * tasks of synchronization and the generation of new blocks must be stopped.
 */
public interface IFork {

  /**
   * Returns genesis-block ID.
   *
   * <p>Genesis block can be considered as a network identifier.
   */
  BlockID getGenesisBlockID();

  /** Returns genesis-block timestamp. */
  int getGenesisBlockTimestamp();

  /**
   * Checks whether the hard-fork has been expired for the specified {@code timestamp}.
   *
   * @param timestamp for which a check is made (unix timestamp)
   * @return true if time not in fork, otherwise false
   */
  boolean isPassed(int timestamp);

  /**
   * Checks whether the hard-fork started at the specified {@code timestamp}.
   *
   * <p>The moment that the fork was started at the specified time, does not guarantee that it was
   * not already completed
   *
   * @param timestamp for which a check is made (unix timestamp)
   * @return true if the hard-fork was started, otherwise - false.
   */
  boolean isCome(int timestamp);

  /**
   * Returns hard-fork number.
   *
   * <p>Hard-forks are numbered sequentially in ascending order.
   *
   * @param timestamp on which it is necessary to calculate the number of the hard-fork (unix
   *     timestamp)
   * @return hard-fork number
   */
  int getNumber(int timestamp);

  /**
   * Returns the known transaction types.
   *
   * @param timestamp for which a types will be returned (unix timestamp)
   * @return set of types
   */
  Set<Integer> getTransactionTypes(int timestamp);

  /**
   * Returns the block version used for the specified time.
   *
   * @param timestamp for which a block version will be returned (unix timestamp)
   * @return block version or -1
   */
  int getBlockVersion(int timestamp);

  /**
   * Returns transactions parser.
   *
   * @return
   */
  ITransactionParser getParser(int timestamp);

  /**
   * Returns block period
   *
   * @param timestamp current time (unix timestamp)
   * @return
   */
  int getBlockPeriod(int timestamp);

  /**
   * Returns block period
   *
   * @param timestamp current time (unix timestamp)
   * @return
   */
  int getNextBlockTimestamp(int timestamp);

  /**
   * Returns block size
   *
   * @param timestamp target time (unix timestamp)
   * @return
   */
  long getBlockSize(int timestamp);

  /**
   * Returns target block height by timestamp
   *
   * @param timestamp target time (unix timestamp)
   * @return
   */
  int getTargetBlockHeight(int timestamp);

  /**
   * Returns salt generation version by timestamp
   *
   * @param timestamp target time (unix timestamp)
   * @return
   */
  int getGenerationSaltVersion(int timestamp);
}
