package com.exscudo.peer.store.sqlite;

/**
 * This interface for an editable object where changes should be commit.
 *
 */
interface ICommitable {

	/**
	 * Commit object changes.
	 */
	void commit();

}
