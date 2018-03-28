package com.exscudo.peer.core.api;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Block;

/**
 * The protocol used to initial synchronization by snapshot.
 */
public interface ISnapshotSynchronizationService {

    /**
     * Returns last block
     *
     * @return
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Block getLastBlock() throws RemotePeerException, IOException;

    /**
     * @param height
     * @return
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Block getBlockByHeight(int height) throws RemotePeerException, IOException;

    /**
     * @param height
     * @return
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Block[] getBlocksHeadFrom(int height) throws RemotePeerException, IOException;

    /**
     * @param blockID
     * @return
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Map<String, Object> getAccounts(String blockID) throws RemotePeerException, IOException;

    /**
     * @param blockID
     * @param accountID
     * @return
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Map<String, Object> getNextAccounts(String blockID, String accountID) throws RemotePeerException, IOException;
}
