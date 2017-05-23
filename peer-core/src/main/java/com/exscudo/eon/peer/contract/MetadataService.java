package com.exscudo.eon.peer.contract;

import java.io.IOException;

import com.exscudo.eon.peer.exceptions.RemotePeerException;

/**
 * The <code>MetadataService</code> interface must implement each node. Used to
 * get the network structure.
 *
 */
public interface MetadataService {

	/**
	 * Gets a remote peer configuration.
	 * 
	 * @param origin
	 *            current node configuration
	 * @return An associative array with the values of the parameters.
	 * @throws RemotePeerException
	 *             An error in the protocol.
	 * @throws IOException
	 *             Error during access to the remote node.
	 */
	@MethodName()
	SalientAttributes getAttributes(SalientAttributes origin) throws RemotePeerException, IOException;

	/**
	 * Gets a list of well-known peers.
	 * 
	 * @return An array of addresses.
	 * @throws RemotePeerException
	 *             An error in the protocol.
	 * @throws IOException
	 *             Error during access to the remote node.
	 */
	@MethodName()
	String[] getWellKnownNodes() throws RemotePeerException, IOException;

}
