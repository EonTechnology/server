package com.exscudo.peer.core;

import java.io.Serializable;

import com.exscudo.peer.core.services.IBlockSynchronizationService;

/**
 * An abstraction for objects that provide access to remote node services.
 */
public interface IPeer extends Serializable {

	/**
	 * Returns peer ID.
	 * 
	 * @return id
	 */
	long getPeerID();

	/**
	 * Creates a stub for the service deployed on the remote node.
	 * 
	 * @param clazz
	 *            service type
	 * @param <TService>
	 *            service type
	 * @return stub
	 * @throws NullPointerException
	 *             if {@code clazz} is null
	 */
	<TService> TService getService(Class<TService> clazz);

	/**
	 * Returns an object that implements the block synchronization protocol
	 * 
	 * @return
	 */
	default IBlockSynchronizationService getBlockSynchronizationService() {
		return getService(IBlockSynchronizationService.class);
	}

}
