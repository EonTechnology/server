package com.exscudo.peer.store.sqlite.lock;

/**
 * Get object to lock.
 */
public interface ILockableObject {

	/**
	 * Returns the identifier.
	 *
	 * @return
	 */
	Object getIdentifier();

}
