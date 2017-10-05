package com.exscudo.peer.eon;

import java.util.HashMap;

import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.eon.ExecutionContext.Host;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.listeners.BacklogCleaner;
import com.exscudo.peer.eon.listeners.BlockGenerator;

public class EngineConfigurator {

	private IBlockchainService blockchain;
	private IBacklogService backlog;

	private ISigner signer;
	private TimeProvider timeProvider;
	private Host host;
	private IServiceProxyFactory proxyFactory;
	private int blacklistingPeriod = 30000;
	private String[] publicPeers;
	private String[] innerPeers;

	private ITransactionHandler txHandler = new TransactionHandlerDecorator(
			new HashMap<Integer, ITransactionHandler>() {
				private static final long serialVersionUID = 3518338953704623292L;

				{
					put(TransactionType.AccountRegistration,
							new com.exscudo.peer.eon.transactions.handlers.AccountRegistrationHandler());
					put(TransactionType.OrdinaryPayment,
							new com.exscudo.peer.eon.transactions.handlers.OrdinaryPaymentHandler());
					put(TransactionType.DepositRefill,
							new com.exscudo.peer.eon.transactions.handlers.DepositRefillHandler());
					put(TransactionType.DepositWithdraw,
							new com.exscudo.peer.eon.transactions.handlers.DepositWithdrawHandler());
				}
			});

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

	public ExecutionContext build() {

		BlockGenerator blockGenerator = new BlockGenerator(backlog, blockchain, txHandler, signer);
		BacklogCleaner cleaner = new BacklogCleaner(backlog, blockchain, txHandler);

		BlockchainDecorator blockchainDecorator = new BlockchainDecorator(blockchain, txHandler);

		blockchainDecorator.addListener(blockGenerator);

		final ExecutionContext context = new ExecutionContext(timeProvider);
		context.addListener(blockGenerator);
		context.addListener(cleaner);

		BacklogDecorator backlogDecorator = new BacklogDecorator(backlog, blockchain, txHandler);

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
				context.addPublicPeer(address);
			}
		}

		context.setProxyFactory(proxyFactory);

		return context;
	}
}
