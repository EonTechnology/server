package com.exscudo.peer.eon.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ValidationMode {
	/**
	 * Minimum weight.
	 */
	public static final int MIN_WEIGHT = 0;

	/**
	 * Maximum weight.
	 */
	public static final int MAX_WEIGHT = 100;

	/**
	 * Minimum allowable quorum.
	 */
	public static final int MIN_QUORUM = 1;

	/**
	 * Maximum allowable quorum.
	 */
	public static final int MAX_QUORUM = 100;

	/**
	 * Percentage of signature for the sender's account
	 */
	private int baseWeight = MIN_WEIGHT;

	/**
	 * Percentage of signatures for delegate accounts
	 */
	private Map<Long, Integer> weights = new HashMap<>();

	/**
	 * Quorum for transactions is used by default
	 */
	private int baseQuorum = MAX_QUORUM;

	/**
	 * Quorum for transactions by type
	 */
	private Map<Integer, Integer> quorums = new HashMap<>();

	/**
	 * Modification time
	 */
	private int timestamp = -1;

	/**
	 * Published SEED. There is a value only if the account is public
	 */
	private String seed;

	public boolean isMultiFactor() {
		return (weights == null || weights.isEmpty()) ? false : true;
	}

	public boolean isPublic() {
		return (isMultiFactor() && seed != null);
	}

	public boolean isNormal() {
		return  !isMultiFactor();
	}

	public void setBaseWeight(int value) {
		if (value < MIN_WEIGHT || value > MAX_WEIGHT) {
			throw new IllegalArgumentException();
		}
		this.baseWeight = value;
	}

	public int getBaseWeight() {
		return baseWeight;
	}

	public int getWeightForAccount(long id) {
		Integer v = weights.get(id);
		if (v == null) {
			throw new IllegalArgumentException();
		}
		return v;
	}

	public void setWeightForAccount(long id, int weight) {
		weights.put(id, weight);
	}

	public int getMaxWeight() {
		int v = getBaseWeight();
		for (int w : weights.values()) {
			v += w;
		}
		return v;
	}

	public boolean containWeightForAccount(long id) {
		return weights.containsKey(id);
	}

	public int getBaseQuorum() {
		return baseQuorum;
	}

	public void setQuorum(int quorum) {
		if (quorum < MIN_QUORUM || quorum > MAX_QUORUM) {
			throw new IllegalArgumentException();
		}
		this.baseQuorum = quorum;
	}

	public void setQuorum(int type, int quorum) {
		if (quorum < MIN_QUORUM || quorum > MAX_QUORUM) {
			throw new IllegalArgumentException();
		}
		quorums.put(type, quorum);
	}

	public int quorumForType(int type) {
		int r = getBaseQuorum();
		if (quorums.containsKey(type)) {
			r = quorums.get(type);
		}
		return r;
	}

	public Set<Map.Entry<Long, Integer>> delegatesEntrySet() {
		return weights.entrySet();
	}

	public Set<Map.Entry<Integer, Integer>> quorumsEntrySet() {
		return quorums.entrySet();
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public void setPublicMode(String seed) {
		this.seed = seed;
	}

	public String getSeed() {
		return seed;
	}

}
