package org.eontechnology.and.peer.core.api;

import java.io.IOException;

import org.eontechnology.and.peer.core.common.exceptions.RemotePeerException;

/**
 * The {@code IMetadataService} interface must implement each env. Used to get
 * the network structure.
 */
public interface IMetadataService {

    /**
     * Gets a services peer configuration.
     *
     * @return An associative array with the values of the parameters.
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services env.
     */
    SalientAttributes getAttributes() throws RemotePeerException, IOException;

    /**
     * Gets a list of connected peers.
     *
     * @return An array of addresses.
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services env.
     */
    String[] getWellKnownNodes() throws RemotePeerException, IOException;

    /**
     * Add new peer to connect
     *
     * @param peerID  peer id
     * @param address peer address (ip:port)
     * @return <code>true</code> if per added to server peer list, otherwise
     * <code>false</code>
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services env.
     */
    boolean addPeer(long peerID, String address) throws RemotePeerException, IOException;
}
