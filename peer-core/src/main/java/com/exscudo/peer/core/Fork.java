package com.exscudo.peer.core;

import java.time.Instant;

/**
 * Hard fork is a pre-planned network update point. At that point in time, new
 * functionality is introduced into the network.
 */
public class Fork {

	protected long BEGIN;
	protected long END;

	protected int FORK = 1;
	private final long genesisBlockID;

	public Fork(long genesisBlockID, String begin, String end) {
		this.genesisBlockID = genesisBlockID;

		BEGIN = Instant.parse(begin).toEpochMilli();
		END = Instant.parse(end).toEpochMilli();
	}

	/**
	 * Returns hard-fork number.
	 * <p>
	 * Hard-forks are numbered sequentially in ascending order.
	 *
	 * @param timestamp
	 *            on which it is necessary to calculate the number of the hard-fork
	 *            (unix timestamp)
	 * @return hard-fork number
	 */
	public int getNumber(int timestamp) {
		if (!isCome(timestamp)) {
			return FORK - 1;
		}
		return FORK;
	}

	public boolean isCome(int timestamp) {
		return timestamp * 1000L > BEGIN;
	}

	/**
	 * Checks whether the time stamp is in the hard-fork range.
	 *
	 * @param timestamp
	 *            for which a check is made (unix timestamp)
	 * @return true if time not in fork, otherwise false
	 */
	public boolean isPassed(int timestamp) {
		return timestamp * 1000L > END;
	}

	/**
	 * Returns genesis-block ID.
	 * <p>
	 * Genesis block can be considered as a network identifier.
	 */
	public long getGenesisBlockID() {
		return genesisBlockID;
	}
}
