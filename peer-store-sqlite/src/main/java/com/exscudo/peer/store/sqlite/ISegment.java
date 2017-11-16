package com.exscudo.peer.store.sqlite;

/**
 * Interface for key-value table
 *
 * @param <TKey>
 *            type of keys.
 * @param <TValue>
 *            base type of objects
 */
public interface ISegment<TKey, TValue> {

	TValue get(TKey id);

	void put(TKey id, TValue instance);

	void remove(TKey id);

	default boolean contains(TKey key) {
		return get(key) != null;
	}

}
