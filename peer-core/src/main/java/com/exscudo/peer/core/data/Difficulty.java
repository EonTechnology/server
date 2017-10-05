package com.exscudo.peer.core.data;

import java.math.BigInteger;

/**
 * The cumulative difficulty of the last block.
 */
public class Difficulty implements Comparable<Difficulty> {

	private BigInteger difficulty;
	private long lastBlockID;

	public Difficulty(Block block) {
		this(block.getID(), block.getCumulativeDifficulty());
	}

	public Difficulty(Difficulty difficulty) {
		this(difficulty.getLastBlockID(), difficulty.getDifficulty());
	}

	public Difficulty(long id, BigInteger difficulty) {
		this.lastBlockID = id;
		this.difficulty = difficulty;
	}

	/**
	 * Returns the identifier of the block.
	 *
	 * @return
	 */
	public long getLastBlockID() {
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

		return Long.compare(getLastBlockID(), o.getLastBlockID());

	}
}
