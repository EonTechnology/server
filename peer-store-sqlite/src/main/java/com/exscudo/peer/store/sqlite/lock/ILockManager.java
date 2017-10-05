package com.exscudo.peer.store.sqlite.lock;

/**
 * Lock strategies
 */
public interface ILockManager {

	/**
	 * Obtain a lock for an object with the specified <code>identifier</code>
	 * 
	 * @param identifier
	 */
	void obtainLock(Object identifier);

	/**
	 * Release the lock for object with the specified <code>identifier</code>
	 * 
	 * @param identifier
	 */
	void releaseLock(Object identifier);

}
