package com.exscudo.peer.eon.services;

import java.io.IOException;

import com.exscudo.peer.core.exceptions.RemotePeerException;

/**
 * The {@code IMetadataService} interface must implement each node. Used to get
 * the network structure.
 *
 */
public interface IMetadataService {

	/**
	 * Gets a services peer configuration.
	 * 
	 * @return An associative array with the values of the parameters.
	 * @throws RemotePeerException
	 *             An error in the protocol.
	 * @throws IOException
	 *             Error during access to the services node.
	 */
	SalientAttributes getAttributes() throws RemotePeerException, IOException;

	/**
	 * Gets a list of connected peers.
	 * 
	 * @return An array of addresses.
	 * @throws RemotePeerException
	 *             An error in the protocol.
	 * @throws IOException
	 *             Error during access to the services node.
	 */
	String[] getWellKnownNodes() throws RemotePeerException, IOException;

}
