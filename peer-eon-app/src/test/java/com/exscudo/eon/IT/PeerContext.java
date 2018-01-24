package com.exscudo.eon.IT;

import java.io.IOException;
import java.sql.SQLException;

import com.exscudo.eon.bot.AccountService;
import com.exscudo.eon.bot.ColoredCoinService;
import com.exscudo.eon.bot.TransactionService;
import com.exscudo.eon.cfg.EngineConfigurator;
import com.exscudo.eon.jsonrpc.ObjectMapperProvider;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.mapper.crypto.SignedObjectMapper;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.services.IBlockSynchronizationService;
import com.exscudo.peer.core.tasks.SyncBlockListTask;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.IServiceProxyFactory;
import com.exscudo.peer.eon.PeerInfo;
import com.exscudo.peer.eon.TimeProvider;
import com.exscudo.peer.eon.crypto.Ed25519SignatureVerifier;
import com.exscudo.peer.eon.crypto.Ed25519Signer;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.services.IMetadataService;
import com.exscudo.peer.eon.services.ITransactionSynchronizationService;
import com.exscudo.peer.eon.services.SalientAttributes;
import com.exscudo.peer.eon.stubs.SyncBlockService;
import com.exscudo.peer.eon.stubs.SyncMetadataService;
import com.exscudo.peer.eon.stubs.SyncTransactionService;
import com.exscudo.peer.eon.tasks.GenerateBlockTask;
import com.exscudo.peer.eon.tasks.PeerConnectTask;
import com.exscudo.peer.eon.tasks.PeerRemoveTask;
import com.exscudo.peer.eon.tasks.SyncPeerListTask;
import com.exscudo.peer.eon.tasks.SyncTransactionListTask;
import com.exscudo.peer.store.sqlite.Storage;
import com.exscudo.peer.store.sqlite.core.Blockchain;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;

class PeerContext {
	ExecutionContext context;//

	PeerRemoveTask peerRemoveTask;
	PeerConnectTask peerConnectTask;
	SyncPeerListTask syncPeerListTask;
	SyncBlockListTask syncBlockListTask;
	GenerateBlockTask generateBlockTask;
	SyncTransactionListTask syncTransactionListTask;

	IBlockSynchronizationService syncBlockPeerService;
	IMetadataService syncMetadataPeerService;
	ITransactionSynchronizationService syncTransactionPeerService;

	TransactionService transactionBotService;
	AccountService accountBotService;
	ColoredCoinService coloredCoinService;

	ISigner signer;
	IFork fork;

	PeerContext(String seed, TimeProvider timeProvider) throws ClassNotFoundException, SQLException, IOException {
		this(seed, timeProvider, Utils.createStorage());
	}

	PeerContext(String seed, TimeProvider timeProvider, Storage storage)
			throws ClassNotFoundException, SQLException, IOException {
		this(seed, timeProvider, storage, Utils.createFork(storage));
	}

	PeerContext(String seed, TimeProvider timeProvider, Storage storage, IFork fork)
			throws SQLException, IOException, ClassNotFoundException {

		final ObjectMapper mapper = ObjectMapperProvider.createDefaultMapper();
		this.signer = new Ed25519Signer(seed);
		this.fork = fork;

		EngineConfigurator configurator = new EngineConfigurator();
		configurator.setHost(new ExecutionContext.Host("0"));
		configurator.setPublicPeers(new String[] { "1" });
		configurator.setInnerPeers(new String[] {});
		configurator.setTimeProvider(timeProvider);
		configurator.setSigner(signer);
		configurator.setFork(fork);
		configurator.setBlockchain(new Blockchain(storage, fork));
		configurator.setBacklog(storage.getBacklog());

		long networkID = fork.getGenesisBlockID();
		Ed25519SignatureVerifier signatureVerifier = new Ed25519SignatureVerifier();
		CryptoProvider cryptoProvider = new CryptoProvider(new SignedObjectMapper(networkID));
		cryptoProvider.addProvider(signatureVerifier);
		cryptoProvider.setDefaultProvider(signatureVerifier.getName());
		CryptoProvider.init(cryptoProvider);

		context = Mockito.spy(configurator.build());

		context.connectPeer(context.getAnyPeerToConnect(), 0);

		generateBlockTask = new GenerateBlockTask(context);
		syncBlockListTask = new SyncBlockListTask(context);

		syncTransactionPeerService = new ITransactionSynchronizationService() {

			private SyncTransactionService inner = new SyncTransactionService(context);

			@Override
			public Transaction[] getTransactions(String lastBlockId, String[] ignoreList)
					throws RemotePeerException, IOException {
				Transaction[] value = inner.getTransactions(lastBlockId, ignoreList);
				String valueStr = mapper.writeValueAsString(value);

				return mapper.readValue(valueStr, Transaction[].class);
			}
		};

		syncBlockPeerService = new IBlockSynchronizationService() {

			private SyncBlockService inner = new SyncBlockService(context);

			@Override
			public Difficulty getDifficulty() throws RemotePeerException, IOException {
				Difficulty value = inner.getDifficulty();
				String valueStr = mapper.writeValueAsString(value);

				return mapper.readValue(valueStr, Difficulty.class);
			}

			@Override
			public Block[] getBlockHistory(String[] blockSequence) throws RemotePeerException, IOException {
				Block[] value = inner.getBlockHistory(blockSequence);
				String valueStr = mapper.writeValueAsString(value);

				return mapper.readValue(valueStr, Block[].class);
			}

			@Override
			public Block getLastBlock() throws RemotePeerException, IOException {
				Block value = inner.getLastBlock();
				String valueStr = mapper.writeValueAsString(value);

				return mapper.readValue(valueStr, Block.class);
			}
		};
		syncMetadataPeerService = new IMetadataService() {
			private SyncMetadataService inner = new SyncMetadataService(context);

			@Override
			public SalientAttributes getAttributes() throws RemotePeerException, IOException {
				SalientAttributes value = inner.getAttributes();
				String valueStr = mapper.writeValueAsString(value);

				return mapper.readValue(valueStr, SalientAttributes.class);
			}

			@Override
			public String[] getWellKnownNodes() throws RemotePeerException, IOException {
				String[] value = inner.getWellKnownNodes();
				String valueStr = mapper.writeValueAsString(value);

				return mapper.readValue(valueStr, String[].class);
			}

			@Override
			public boolean addPeer(long peerID, String address) throws RemotePeerException, IOException {
				return inner.addPeer(peerID, address);
			}

		};

		accountBotService = new AccountService(storage);
		transactionBotService = new TransactionService(context);
		syncTransactionListTask = new SyncTransactionListTask(context);
		coloredCoinService = new ColoredCoinService(storage);

		syncPeerListTask = new SyncPeerListTask(context);

		peerConnectTask = new PeerConnectTask(context);
		peerRemoveTask = new PeerRemoveTask(context);
	}

	public void generateBlockForNow() {
		Block lastBlock = context.getInstance().getBlockchainService().getLastBlock();
		long lastBlockID = 0;

		do {
			lastBlockID = lastBlock.getID();

			context.getInstance().getGenerator().allowGenerate();
			generateBlockTask.run();

			lastBlock = context.getInstance().getBlockchainService().getLastBlock();
		} while (lastBlock.getTimestamp() + Constant.BLOCK_PERIOD < context.getCurrentTime()
				&& lastBlockID != lastBlock.getID());
	}

	public void fullBlockSync() {
		long lastBlockID;
		do {
			lastBlockID = context.getInstance().getBlockchainService().getLastBlock().getID();
			syncBlockListTask.run();
		} while (context.getInstance().getBlockchainService().getLastBlock().getID() != lastBlockID);
	}

	public void setPeerToConnect(PeerContext ctx) {
		context.setProxyFactory(new IServiceProxyFactory() {
			@SuppressWarnings("unchecked")
			@Override
			public <TService> TService createProxy(PeerInfo peer, Class<TService> clazz) {
				if (clazz.equals(IBlockSynchronizationService.class))
					return (TService) ctx.syncBlockPeerService;
				if (clazz.equals(ITransactionSynchronizationService.class))
					return (TService) ctx.syncTransactionPeerService;
				if (clazz.equals(IMetadataService.class))
					return (TService) ctx.syncMetadataPeerService;

				return null;
			}
		});
	}

	public ISigner getSigner() {
		return signer;
	}
}
