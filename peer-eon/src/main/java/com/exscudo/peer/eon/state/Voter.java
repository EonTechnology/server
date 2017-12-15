package com.exscudo.peer.eon.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Voter {

	/**
	 * Contains a list of votings that the account is participated.
	 */
	private Map<Long, Integer> votePolls = new HashMap<>();

	/**
	 * Modification time
	 */
	private int timestamp = -1;

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public void setPoll(long accountID, int votes) {
		votePolls.put(accountID, votes);
	}

	public Set<Map.Entry<Long, Integer>> pollsEntrySet() {
		return votePolls.entrySet();
	}

	public boolean hasPolls() {
		return !votePolls.isEmpty();
	}

}
