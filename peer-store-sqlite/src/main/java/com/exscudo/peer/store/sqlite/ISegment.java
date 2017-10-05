package com.exscudo.peer.store.sqlite;

/**
 * Interface for caching changes
 * 
 * @param <T>
 *            base type of cached objects
 */
public interface ISegment<T> {
	T get(long id);
	void put(T instance);
	void remove(long id);
	boolean contains(long id);
	void commit();
}
