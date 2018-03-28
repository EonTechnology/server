package com.exscudo.eon.jsonrpc.proxy;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.api.IBlockSynchronizationService;
import com.exscudo.peer.core.api.ISnapshotSynchronizationService;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.env.IServiceProxyFactory;

/**
 * Proxy for {@code IBlockSynchronizationService} on remote peer
 * <p>
 * To create use {@link IServiceProxyFactory#createProxy}
 *
 * @see IBlockSynchronizationService
 */
public class SnapshotSynchronizationServiceProxy extends PeerServiceProxy implements ISnapshotSynchronizationService {

    @Override
    public Block getLastBlock() throws RemotePeerException, IOException {
        return doRequest("getLastBlock", new Object[0], Block.class);
    }

    @Override
    public Block getBlockByHeight(int height) throws RemotePeerException, IOException {
        return doRequest("getBlockByHeight", new Object[] {height}, Block.class);
    }

    @Override
    public Block[] getBlocksHeadFrom(int height) throws RemotePeerException, IOException {
        return doRequest("getBlocksHeadFrom", new Object[] {height}, Block[].class);
    }

    @Override
    public Map<String, Object> getAccounts(String blockID) throws RemotePeerException, IOException {
        return doRequest("getAccounts", new Object[] {blockID}, Map.class);
    }

    @Override
    public Map<String, Object> getNextAccounts(String blockID,
                                               String accountID) throws RemotePeerException, IOException {
        return doRequest("getNextAccounts", new Object[] {blockID, accountID}, Map.class);
    }
}
