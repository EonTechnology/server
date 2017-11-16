package com.exscudo.peer.core.services;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.exceptions.ValidateException;

/**
 * Provides access to the editing functions of the chain.
 * <p>
 * All changes within of one task are either accepted or rolled back.
 *
 */
public interface IUnitOfWork {

	/**
	 * Adds the passed {@code block} to the tail of the chain
	 * 
	 * @param block
	 * @return new last block
	 * @throws ValidateException
	 *             if some property of the specified {@code block} prevents it from
	 *             being pushing.
	 */
	Block pushBlock(Block block) throws ValidateException;

	/**
	 * Commits the IUnitOfWork.
	 */
	void commit();

	/**
	 * Roll backs the IUnitOfWork.
	 */
	void rollback();

}
