package org.eontechnology.and.peer.core.ledger.storage;

import java.util.Collections;
import java.util.Map;
import org.eontechnology.and.peer.core.common.CachedHashMap;

public class DbNodeCache {
  private final CachedHashMap<Long, DbNode> map = new CachedHashMap<Long, DbNode>(100000);
  private final Map<Long, DbNode> cache = Collections.synchronizedMap(map);

  public DbNodeCache() {}

  public DbNode get(long id) {
    return cache.get(id);
  }

  public void put(long id, DbNode node) {
    cache.put(id, node);
  }

  public void remove(long id) {
    cache.remove(id);
  }

  public int getSize() {
    return cache.size();
  }

  public long getAdded() {
    return map.getAdded();
  }

  public long getRemoved() {
    return map.getRemoved();
  }
}
