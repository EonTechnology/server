package com.exscudo.peer.core;

import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.IBlockchainService;

/**
 * An abstraction for objects that provides a set of services for accessing data
 * on the current node.
 */
public interface IInstance {

	/**
	 * Returns the interface to work with the chain of blocks.
	 * 
	 * @return blockchain
	 */
	IBlockchainService getBlockchainService();

	/**
	 * Returns the Backlog interface.
	 * 
	 * @return backlog
	 */
	IBacklogService getBacklogService();
}
