package com.exscudo.eon.cfg;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.eon.BacklogDecorator;
import com.exscudo.peer.eon.BlockchainDecorator;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.ExecutionContext.Host;
import com.exscudo.peer.eon.IServiceProxyFactory;
import com.exscudo.peer.eon.Instance;
import com.exscudo.peer.eon.TimeProvider;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.listeners.BacklogCleaner;
import com.exscudo.peer.eon.listeners.BlockGenerator;

public class EngineConfigurator {

	private IBlockchainService blockchain;
	private IBacklogService backlog;
	private IFork fork;

	private ISigner signer;
	private TimeProvider timeProvider;
	private Host host;
	private IServiceProxyFactory proxyFactory;
	private int blacklistingPeriod = 30000;
	private String[] publicPeers;
	private String[] innerPeers;

	public EngineConfigurator setSigner(ISigner signer) {
		this.signer = signer;
		return this;
	}

	public EngineConfigurator setBlockchain(IBlockchainService blockchain) {
		this.blockchain = blockchain;
		return this;
	}

	public EngineConfigurator setBacklog(IBacklogService backlog) {
		this.backlog = backlog;
		return this;
	}

	public EngineConfigurator setTimeProvider(TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
		return this;
	}

	public EngineConfigurator setHost(Host host) {
		this.host = host;
		return this;
	}

	public EngineConfigurator setProxyFactory(IServiceProxyFactory proxyFactory) {
		this.proxyFactory = proxyFactory;
		return this;
	}

	public EngineConfigurator setInnerPeers(String[] addresses) {
		this.innerPeers = addresses;
		return this;
	}

	public EngineConfigurator setPublicPeers(String[] addresses) {
		this.publicPeers = addresses;
		return this;

	}

	public EngineConfigurator setBlacklistingPeriod(int blacklistingPeriod) {
		this.blacklistingPeriod = blacklistingPeriod;
		return this;
	}

	public EngineConfigurator setFork(IFork fork) {
		this.fork = fork;
		return this;
	}

	public ExecutionContext build() {

		BlockGenerator blockGenerator = new BlockGenerator(backlog, blockchain, signer);
		BacklogCleaner cleaner = new BacklogCleaner(backlog, blockchain, fork);

		BlockchainDecorator blockchainDecorator = new BlockchainDecorator(blockchain);
		blockchainDecorator.addListener(blockGenerator);
		blockchainDecorator.addListener(cleaner);

		final ExecutionContext context = new ExecutionContext(timeProvider, fork);
		context.addListener(blockGenerator);

		BacklogDecorator backlogDecorator = new BacklogDecorator(backlog, blockchain, fork);

		Instance peer = new Instance(blockchainDecorator, backlogDecorator, blockGenerator);

		context.setPeer(peer);
		context.setHost(host);
		context.setBlacklistingPeriod(blacklistingPeriod);

		for (String address : innerPeers) {
			address = address.trim();
			if (address.length() > 0) {
				context.addInnerPeer(address);
			}
		}

		for (String address : publicPeers) {
			address = address.trim();
			if (address.length() > 0) {
				context.addImmutablePeer(address);
			}
		}

		context.setProxyFactory(proxyFactory);

		return context;
	}

}
