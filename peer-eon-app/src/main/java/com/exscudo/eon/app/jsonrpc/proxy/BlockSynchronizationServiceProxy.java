package com.exscudo.eon.app.jsonrpc.proxy;

import java.io.IOException;

import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.api.IBlockSynchronizationService;
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
public class BlockSynchronizationServiceProxy extends PeerServiceProxy implements IBlockSynchronizationService {

    @Override
    public Difficulty getDifficulty() throws RemotePeerException, IOException {
        return doRequest("get_difficulty", new Object[0], Difficulty.class);
    }

    @Override
    public Block[] getBlockHistory(String[] blockSequence) throws RemotePeerException, IOException {
        return doRequest("get_block_history", new Object[] {blockSequence}, Block[].class);
    }

    @Override
    public Block getLastBlock() throws RemotePeerException, IOException {
        return doRequest("get_last_block", new Object[0], Block.class);
    }
}
