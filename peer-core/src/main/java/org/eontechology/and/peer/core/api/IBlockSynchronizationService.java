package org.eontechology.and.peer.core.api;

import java.io.IOException;

import org.eontechology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechology.and.peer.core.data.Block;

/**
 * The protocol used to synchronize a block.
 */
public interface IBlockSynchronizationService {

    /**
     * Returns the current "difficulty" of the chain.
     *
     * @return
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Difficulty getDifficulty() throws RemotePeerException, IOException;

    /**
     * Returns an array of the last blocks.
     *
     * @param blockSequence IDs of the last blocks.
     * @return
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Block[] getBlockHistory(String[] blockSequence) throws RemotePeerException, IOException;

    /**
     * Returns last block
     *
     * @return
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Block getLastBlock() throws RemotePeerException, IOException;
}
