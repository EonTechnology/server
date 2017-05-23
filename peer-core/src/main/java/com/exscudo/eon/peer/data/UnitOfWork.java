package com.exscudo.eon.peer.data;

/**
 * This interface represents a simple UnitOfWork.
 *
 */
public interface UnitOfWork {

	/**
	 * Commits the UnitOfWork.
	 */
	void apply();

	/**
	 * Roll backs the UnitOfWork.
	 */
	void restore();

}
