package org.eontechnology.and.peer.core.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base cache implementation for the fix number of records
 *
 * @param <K> type of key
 * @param <V> type of value
 */
public class CachedHashMap<K, V> extends LinkedHashMap<K, V> {
  private static final long serialVersionUID = -1756745314063573436L;

  private final int size;
  private volatile AtomicLong added = new AtomicLong();
  private volatile AtomicLong removed = new AtomicLong();

  public CachedHashMap(int size) {
    super(0, 0.75f, true);
    this.size = size;
  }

  @Override
  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    added.incrementAndGet();
    if (size() > size) {
      removed.incrementAndGet();
      return true;
    }
    return false;
  }

  public long getAdded() {
    return added.get();
  }

  public long getRemoved() {
    return removed.get();
  }
}
