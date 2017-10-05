package com.exscudo.eon.jsonrpc.proxy;

import java.io.IOException;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.services.IBlockSynchronizationService;
import com.exscudo.peer.eon.IServiceProxyFactory;

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
		return doRequest("getDifficulty", new Object[0], Difficulty.class);
	}

	@Override
	public Block[] getBlockHistory(String[] blockSequence) throws RemotePeerException, IOException {
		return doRequest("getBlockHistory", new Object[]{blockSequence}, Block[].class);
	}

	@Override
	public Block getLastBlock() throws RemotePeerException, IOException {
		return doRequest("getLastBlock", new Object[0], Block.class);
	}

}
