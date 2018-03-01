package com.exscudo.eon.IT;

import java.io.IOException;
import java.sql.SQLException;

import com.exscudo.eon.bot.AccountService;
import com.exscudo.eon.bot.ColoredCoinService;
import com.exscudo.eon.bot.TransactionService;
import com.exscudo.eon.cfg.*;
import com.exscudo.eon.jsonrpc.ObjectMapperProvider;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.api.*;
import com.exscudo.peer.core.api.impl.SyncBlockService;
import com.exscudo.peer.core.api.impl.SyncMetadataService;
import com.exscudo.peer.core.api.impl.SyncTransactionService;
import com.exscudo.peer.core.backlog.Backlog;
import com.exscudo.peer.core.backlog.tasks.SyncForkedTransactionListTask;
import com.exscudo.peer.core.backlog.tasks.SyncTransactionListTask;
import com.exscudo.peer.core.blockchain.IBlockchainService;
import com.exscudo.peer.core.blockchain.TransactionProvider;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.env.IServiceProxyFactory;
import com.exscudo.peer.core.env.PeerInfo;
import com.exscudo.peer.core.env.tasks.PeerConnectTask;
import com.exscudo.peer.core.env.tasks.PeerRemoveTask;
import com.exscudo.peer.core.env.tasks.SyncPeerListTask;
import com.exscudo.peer.core.importer.BlockGenerator;
import com.exscudo.peer.core.importer.IFork;
import com.exscudo.peer.core.importer.tasks.GenerateBlockTask;
import com.exscudo.peer.core.importer.tasks.SyncBlockListTask;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.core.storage.Storage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;

class PeerContext {
    Backlog backlog;
    BlockGenerator generator;
    ExecutionContext context;
    TimeProvider timeProvider;
    IBlockchainService blockchain;
    TransactionProvider transactionProvider;
    LedgerProvider ledgerProvider;

    PeerRemoveTask peerRemoveTask;
    PeerConnectTask peerConnectTask;
    SyncPeerListTask syncPeerListTask;
    SyncBlockListTask syncBlockListTask;
    GenerateBlockTask generateBlockTask;
    SyncTransactionListTask syncTransactionListTask;
    SyncForkedTransactionListTask syncForkedTransactionListTask;

    IBlockSynchronizationService syncBlockPeerService;
    IMetadataService syncMetadataPeerService;
    ITransactionSynchronizationService syncTransactionPeerService;

    TransactionService transactionBotService;
    AccountService accountBotService;
    ColoredCoinService coloredCoinService;

    ISigner signer;
    IFork fork;
    Storage storage;

    PeerContext(String seed, TimeProvider timeProvider) throws ClassNotFoundException, SQLException, IOException {
        this(seed, timeProvider, Utils.createStorage());
    }

    PeerContext(String seed,
                TimeProvider timeProvider,
                Storage storage) throws ClassNotFoundException, SQLException, IOException {
        this(seed, timeProvider, storage, Utils.createFork(storage));
    }

    PeerContext(String seed,
                TimeProvider timeProvider,
                Storage storage,
                IFork fork) throws SQLException, IOException, ClassNotFoundException {

        // Init peer configuration
        Config config = new Config();
        config.setHost("0");
        config.setBlacklistingPeriod(30000);
        config.setPublicPeers(new String[] {"1"});
        config.setSeed(seed);

        // Init PeerStarter
        PeerStarter starter = new PeerStarter(config);
        starter.setTimeProvider(timeProvider);
        starter.setFork(fork);
        starter.setStorage(storage);

        // Init crypto provider
        CryptoProvider.init(starter.getCryptoProvider());

        // Init context
        context = starter.getExecutionContext();
        context.connectPeer(context.getAnyPeerToConnect(), 0);
        context = Mockito.spy(context);
        starter.setExecutionContext(context);

        // Factories
        BotServiceFactory botServiceFactory = new BotServiceFactory(starter);
        PeerServiceFactory peerServiceFactory = new PeerServiceFactory(starter);
        TaskFactory taskFactory = new TaskFactory(starter);

        // Init local fields
        this.signer = starter.getSigner();
        this.fork = starter.getFork();
        this.storage = starter.getStorage();
        this.blockchain = starter.getBlockchain();
        this.backlog = starter.getBacklog();
        this.generator = starter.getBlockGenerator();
        this.timeProvider = starter.getTimeProvider();
        this.transactionProvider = starter.getTransactionProvider();
        this.ledgerProvider = starter.getLedgerProvider();

        starter.getCleaner().setLogging(false);

        // Init bot services
        accountBotService = botServiceFactory.getAccountService();
        transactionBotService = botServiceFactory.getTransactionService();
        coloredCoinService = botServiceFactory.getColoredCoinService();

        // Init peer tasks
        syncTransactionListTask = taskFactory.getSyncTransactionListTask();
        syncForkedTransactionListTask = taskFactory.getSyncForkedTransactionListTask();
        syncPeerListTask = taskFactory.getSyncPeerListTask();
        peerConnectTask = taskFactory.getPeerConnectTask();
        peerRemoveTask = taskFactory.getPeerRemoveTask();
        generateBlockTask = taskFactory.getGenerateBlockTask();
        syncBlockListTask = taskFactory.getSyncBlockListTask();

        final ObjectMapper mapper = ObjectMapperProvider.createMapper();
        syncTransactionPeerService = new ITransactionSynchronizationService() {

            private SyncTransactionService inner = peerServiceFactory.getSyncTransactionService();

            @Override
            public Transaction[] getTransactions(String lastBlockId,
                                                 String[] ignoreList) throws RemotePeerException, IOException {
                Transaction[] value = inner.getTransactions(lastBlockId, ignoreList);
                String valueStr = mapper.writeValueAsString(value);

                return mapper.readValue(valueStr, Transaction[].class);
            }
        };

        syncBlockPeerService = new IBlockSynchronizationService() {

            private SyncBlockService inner = peerServiceFactory.getSyncBlockService();

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
            private SyncMetadataService inner = peerServiceFactory.getSyncMetadataService();

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
    }

    public void generateBlockForNow() {
        Block lastBlock = blockchain.getLastBlock();
        BlockID lastBlockID;

        do {
            lastBlockID = lastBlock.getID();

            generator.allowGenerate();
            generateBlockTask.run();

            lastBlock = blockchain.getLastBlock();
        } while (lastBlock.getTimestamp() + Constant.BLOCK_PERIOD < timeProvider.get() &&
                !lastBlockID.equals(lastBlock.getID()));
    }

    public void fullBlockSync() {
        BlockID lastBlockID;
        do {
            lastBlockID = blockchain.getLastBlock().getID();
            syncBlockListTask.run();
        } while (!blockchain.getLastBlock().getID().equals(lastBlockID));
    }

    public void setPeerToConnect(PeerContext ctx) {
        context.setProxyFactory(new IServiceProxyFactory() {
            @SuppressWarnings("unchecked")
            @Override
            public <TService> TService createProxy(PeerInfo peer, Class<TService> clazz) {
                if (clazz.equals(IBlockSynchronizationService.class)) {
                    return (TService) ctx.syncBlockPeerService;
                }
                if (clazz.equals(ITransactionSynchronizationService.class)) {
                    return (TService) ctx.syncTransactionPeerService;
                }
                if (clazz.equals(IMetadataService.class)) {
                    return (TService) ctx.syncMetadataPeerService;
                }

                return null;
            }
        });
    }

    public ISigner getSigner() {
        return signer;
    }
}
