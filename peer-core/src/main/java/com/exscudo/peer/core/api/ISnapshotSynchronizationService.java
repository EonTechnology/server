package com.exscudo.peer.core.api;

import java.io.IOException;

import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Block;

/**
 * The protocol used to initial synchronization by snapshot.
 */
public interface ISnapshotSynchronizationService {

    /**
     * Returns the last block in the chain.
     *
     * @return
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Block getLastBlock() throws RemotePeerException, IOException;

    /**
     * Returns the block by the specified <code>height</code>.
     * <p>The block is returned together with the transactions.
     *
     * @param height
     * @return block or null
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Block getBlockByHeight(int height) throws RemotePeerException, IOException;

    /**
     * Returns the block header by the specified <code>height</code>.
     * <p>The block is returned without the transactions.
     *
     * @param height
     * @return block or null
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Block[] getBlocksHeadFrom(int height) throws RemotePeerException, IOException;

    /**
     * Returns accounts states at the moment determined by the block.
     *
     * @param blockID
     * @return
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Account[] getAccounts(String blockID) throws RemotePeerException, IOException;

    /**
     * Returns next accounts states at the moment determined by the block.
     *
     * @param blockID
     * @param accountID
     * @return
     * @throws RemotePeerException An error in the protocol.
     * @throws IOException         Error during access to the services node.
     */
    Account[] getNextAccounts(String blockID, String accountID) throws RemotePeerException, IOException;
}
