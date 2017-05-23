package com.exscudo.eon.peer.data;

/**
 * Provides synchronization object.
 * 
 * synchronized(someObject.syncObject()) { // do something... }
 * 
 */
public interface Synchronizable {

	/**
	 * Get synchronization object.
	 * 
	 * @return Object, can not be null.
	 */
	Object syncObject();

}
