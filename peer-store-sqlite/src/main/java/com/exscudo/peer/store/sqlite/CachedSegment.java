package com.exscudo.peer.store.sqlite;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of {@code ISegment} with data caching.
 * 
 * @param <TKey>
 *            type of keys.
 * @param <TValue>
 *            base type of objects
 */
public abstract class CachedSegment<TKey, TValue> implements ISegment<TKey, TValue>, ICommitable {

	protected HashSet<TKey> removed = new HashSet<>();
	protected Map<TKey, TValue> saved = new ConcurrentHashMap<>();

	protected abstract TValue doGet(TKey id);

	protected abstract void doPut(TKey id, TValue value);

	protected abstract void doRemove(TKey id);

	@Override
	public TValue get(TKey id) {
		Objects.requireNonNull(id);

		if (this.removed.contains(id)) {
			return null;
		}

		if (this.saved.containsKey(id)) {
			return this.saved.get(id);
		}

		return doGet(id);

	}

	@Override
	public void put(TKey id, TValue value) {
		Objects.requireNonNull(id);
		Objects.requireNonNull(value);

		saved.put(id, value);
		removed.remove(id);

	}

	@Override
	public void remove(TKey id) {
		Objects.requireNonNull(id);

		saved.remove(id);
		removed.add(id);

	}

	@Override
	public void commit() {

		for (TKey id : removed) {
			doRemove(id);
		}
		for (Map.Entry<TKey, TValue> entry : saved.entrySet()) {
			doPut(entry.getKey(), entry.getValue());
		}

	}

}
