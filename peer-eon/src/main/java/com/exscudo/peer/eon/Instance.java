package com.exscudo.peer.eon;

import com.exscudo.peer.core.IInstance;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.eon.listeners.BlockGenerator;

/**
 * Provides an access to a set of services on the current node.
 */
public class Instance implements IInstance {

	private final IBacklogService backlog;
	private final IBlockchainService blockchain;
	private final BlockGenerator generator;

	public Instance(IBlockchainService blockchain, IBacklogService backlog, BlockGenerator generator) {
		this.blockchain = blockchain;
		this.backlog = backlog;
		this.generator = generator;
	}

	public BlockGenerator getGenerator() {
		return generator;
	}

	@Override
	public IBacklogService getBacklogService() {
		return backlog;
	}

	@Override
	public IBlockchainService getBlockchainService() {
		return blockchain;
	}

}
