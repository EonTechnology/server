package com.exscudo.peer.core.ledger.storage;

import java.util.Collections;
import java.util.Map;

import com.exscudo.peer.core.common.CachedHashMap;

public class DbNodeCache {
    private final Map<Long, DbNode> cache = Collections.synchronizedMap(new CachedHashMap<Long, DbNode>(100000));

    public DbNodeCache() {
    }

    public DbNode get(long id) {
        return cache.get(id);
    }

    public void put(long id, DbNode node) {
        cache.put(id, node);
    }

    public void remove(long id) {
        cache.remove(id);
    }
}
