package org.eontechnology.and.peer.core.api;

import java.math.BigInteger;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.identifier.BlockID;

/** The cumulative difficulty of the last block. */
public class Difficulty implements Comparable<Difficulty> {

  private BigInteger difficulty;
  private BlockID lastBlockID;

  public Difficulty(Block block) {
    this(block.getID(), block.getCumulativeDifficulty());
  }

  public Difficulty(Difficulty difficulty) {
    this(difficulty.getLastBlockID(), difficulty.getDifficulty());
  }

  public Difficulty(BlockID id, BigInteger difficulty) {
    this.lastBlockID = id;
    this.difficulty = difficulty;
  }

  /**
   * Returns the identifier of the block.
   *
   * @return
   */
  public BlockID getLastBlockID() {
    return lastBlockID;
  }

  /**
   * Returns the "difficulty" of the block.
   *
   * @return
   */
  public BigInteger getDifficulty() {
    return difficulty;
  }

  @Override
  public int compareTo(Difficulty o) {

    if (getDifficulty() == null || o.getDifficulty() == null) {
      throw new IllegalStateException("Invalid Cumulative Difficulty value.");
    }

    int value = getDifficulty().compareTo(o.getDifficulty());
    if (value != 0) {
      return value;
    }

    return Long.compare(getLastBlockID().getValue(), o.getLastBlockID().getValue());
  }
}
