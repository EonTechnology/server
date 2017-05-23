package com.exscudo.eon.peer.contract;

import java.math.BigInteger;

import com.exscudo.eon.peer.data.Block;

/**
 * The cumulative difficulty of the last block.
 *
 */
public class Difficulty implements Comparable<Difficulty> {

	private BigInteger difficulty;
	private long lastBlockID;

	public Difficulty() {
	}
	
	public Difficulty(Block block) {
		this(block.getID(), block.getCumulativeDifficulty());
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

		if (getDifficulty() == null && o.getDifficulty() == null) {

			if (getLastBlockID() > o.getLastBlockID()) {
				return 1;
			} else if (getLastBlockID() == o.getLastBlockID()) {
				return 0;
			}

		} else if (getDifficulty() != null) {

			if (o.getDifficulty() == null) {
				return 1;
			}

			int value = getDifficulty().compareTo(o.getDifficulty());
			if (value != 0) {
				return value;
			}

			if (getLastBlockID() > o.getLastBlockID()) {
				return 1;
			} else if (getLastBlockID() == o.getLastBlockID()) {
				return 0;
			}
		}

		return -1;

	}
}
