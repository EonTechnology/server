package org.eontechnology.and.peer.eon.ledger.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.eontechnology.and.peer.core.data.identifier.AccountID;

public class ValidationModeProperty {

  /** Minimum weight. */
  public static final int MIN_WEIGHT = 0;

  /** Maximum weight. */
  public static final int MAX_WEIGHT = 100;

  /** Minimum allowable quorum. */
  public static final int MIN_QUORUM = 1;

  /** Maximum allowable quorum. */
  public static final int MAX_QUORUM = 100;

  /** Percentage of signature for the sender's account */
  private int baseWeight = MAX_WEIGHT;

  /** Percentage of signatures for delegate accounts */
  private Map<AccountID, Integer> weights = new HashMap<>();

  /** Quorum for transactions is used by default */
  private int baseQuorum = MAX_QUORUM;

  /** Quorum for transactions by type */
  private Map<Integer, Integer> quorums = new HashMap<>();

  /** Modification time */
  private int timestamp = -1;

  /** Published SEED. There is a value only if the account is public */
  private String seed = null;

  public boolean isMultiFactor() {
    return !weights.isEmpty();
  }

  public boolean isPublic() {
    return (isMultiFactor() && seed != null);
  }

  public boolean isNormal() {
    return !isMultiFactor();
  }

  public int getBaseWeight() {
    return baseWeight;
  }

  public void setBaseWeight(int value) {
    if (value < MIN_WEIGHT || value > MAX_WEIGHT) {
      throw new IllegalArgumentException();
    }
    this.baseWeight = value;
  }

  public int getWeightForAccount(AccountID id) {
    Integer v = weights.get(id);
    if (v == null) {
      throw new IllegalArgumentException();
    }
    return v;
  }

  public ValidationModeProperty setWeightForAccount(AccountID id, int weight) {
    weights.put(id, weight);
    return this;
  }

  public int getMaxWeight() {
    int v = getBaseWeight();
    for (int w : weights.values()) {
      v += w;
    }
    return v;
  }

  public boolean containWeightForAccount(AccountID id) {
    return weights.containsKey(id);
  }

  public int getBaseQuorum() {
    return baseQuorum;
  }

  public ValidationModeProperty setQuorum(int quorum) {
    if (quorum < MIN_QUORUM || quorum > MAX_QUORUM) {
      throw new IllegalArgumentException();
    }
    this.baseQuorum = quorum;
    return this;
  }

  public ValidationModeProperty setQuorum(int type, int quorum) {
    if (quorum < MIN_QUORUM || quorum > MAX_QUORUM) {
      throw new IllegalArgumentException();
    }
    quorums.put(type, quorum);
    return this;
  }

  public int quorumForType(int type) {
    int r = getBaseQuorum();
    if (quorums.containsKey(type)) {
      r = quorums.get(type);
    }
    return r;
  }

  public Set<Map.Entry<AccountID, Integer>> delegatesEntrySet() {
    return weights.entrySet();
  }

  public Set<Map.Entry<Integer, Integer>> quorumsEntrySet() {
    return quorums.entrySet();
  }

  public int getTimestamp() {
    return timestamp;
  }

  public ValidationModeProperty setTimestamp(int timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public ValidationModeProperty setPublicMode(String seed) {
    this.seed = seed;
    return this;
  }

  public String getSeed() {
    return seed;
  }
}
