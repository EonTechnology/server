package com.exscudo.eon.IT;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import com.exscudo.eon.api.BacklogService;
import com.exscudo.eon.api.BlockService;
import com.exscudo.eon.api.TransactionService;
import com.exscudo.eon.api.bot.AccountBotService;
import com.exscudo.eon.api.bot.ColoredCoinBotService;
import com.exscudo.eon.api.bot.TransactionBotService;
import com.exscudo.eon.cfg.BotServiceFactory;
import com.exscudo.eon.cfg.Config;
import com.exscudo.eon.cfg.PeerServiceFactory;
import com.exscudo.eon.cfg.PeerStarter;
import com.exscudo.eon.cfg.TaskFactory;
import com.exscudo.eon.jsonrpc.ObjectMapperProvider;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.api.IBlockSynchronizationService;
import com.exscudo.peer.core.api.IMetadataService;
import com.exscudo.peer.core.api.ISnapshotSynchronizationService;
import com.exscudo.peer.core.api.ITransactionSynchronizationService;
import com.exscudo.peer.core.api.SalientAttributes;
import com.exscudo.peer.core.api.impl.SyncBlockService;
import com.exscudo.peer.core.api.impl.SyncMetadataService;
import com.exscudo.peer.core.api.impl.SyncSnapshotService;
import com.exscudo.peer.core.api.impl.SyncTransactionService;
import com.exscudo.peer.core.backlog.Backlog;
import com.exscudo.peer.core.backlog.BacklogCleaner;
import com.exscudo.peer.core.backlog.tasks.SyncForkedTransactionListTask;
import com.exscudo.peer.core.backlog.tasks.SyncTransactionListTask;
import com.exscudo.peer.core.blockchain.IBlockchainProvider;
import com.exscudo.peer.core.blockchain.TransactionProvider;
import com.exscudo.peer.core.blockchain.tasks.BlockCleanerTask;
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
import com.exscudo.peer.core.importer.tasks.GenerateBlockTask;
import com.exscudo.peer.core.importer.tasks.SyncBlockListTask;
import com.exscudo.peer.core.importer.tasks.SyncSnapshotTask;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.core.ledger.tasks.NodesCleanupTask;
import com.exscudo.peer.core.storage.Storage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;

class PeerContext {

    BlockGenerator generator;
    Backlog backlog;
    IBlockchainProvider blockchain;
    TransactionProvider transactionProvider;
    LedgerProvider ledgerProvider;
    BacklogCleaner backlogCleaner;

    ExecutionContext context;
    TimeProvider timeProvider;

    PeerRemoveTask peerRemoveTask;
    PeerConnectTask peerConnectTask;
    SyncPeerListTask syncPeerListTask;
    SyncBlockListTask syncBlockListTask;
    SyncSnapshotTask syncSnapshotTask;
    GenerateBlockTask generateBlockTask;
    SyncTransactionListTask syncTransactionListTask;
    SyncForkedTransactionListTask syncForkedTransactionListTask;
    BlockCleanerTask branchesCleanupTask;
    NodesCleanupTask nodesCleanupTask;

    IBlockSynchronizationService syncBlockPeerService;
    IMetadataService syncMetadataPeerService;
    ITransactionSynchronizationService syncTransactionPeerService;
    ISnapshotSynchronizationService syncSnapshotService;

    TransactionBotService transactionBotService;
    AccountBotService accountBotService;
    ColoredCoinBotService coloredCoinService;

    BacklogService backlogExplorerService;
    BlockService blockExplorerService;
    TransactionService transactionExplorerService;

    ISigner signer;
    IFork fork;
    Storage storage;

    PeerContext(String seed, TimeProvider timeProvider) throws ClassNotFoundException, SQLException, IOException {
        this(seed, timeProvider, Utils.createStorage());
    }

    PeerContext(String seed,
                TimeProvider timeProvider,
                boolean fullHistory) throws ClassNotFoundException, SQLException, IOException {
        this(seed, timeProvider, Utils.createStorage(fullHistory));
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
        Utils.setMockCryptoProvider();

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
        this.blockchain = starter.getBlockchainProvider();
        this.backlog = starter.getBacklog();
        this.generator = starter.getBlockGenerator();
        this.timeProvider = starter.getTimeProvider();
        this.transactionProvider = starter.getTransactionProvider();
        this.ledgerProvider = starter.getLedgerProvider();
        this.backlogCleaner = starter.getCleaner();

        // Init listeners
        starter.getBlockEventManager().addListener(starter.getCleaner());

        // Init bot services
        accountBotService = botServiceFactory.getAccountBotService();
        transactionBotService = botServiceFactory.getTransactionBotService();
        coloredCoinService = botServiceFactory.getColoredCoinBotService();

        backlogExplorerService = botServiceFactory.getBacklogService();
        blockExplorerService = botServiceFactory.getBlockService();
        transactionExplorerService = botServiceFactory.getTransactionService();

        // Init peer tasks
        syncTransactionListTask = taskFactory.getSyncTransactionListTask();
        syncForkedTransactionListTask = taskFactory.getSyncForkedTransactionListTask();
        syncPeerListTask = taskFactory.getSyncPeerListTask();
        peerConnectTask = taskFactory.getPeerConnectTask();
        peerRemoveTask = taskFactory.getPeerRemoveTask();
        generateBlockTask = taskFactory.getGenerateBlockTask();
        syncBlockListTask = taskFactory.getSyncBlockListTask();
        branchesCleanupTask = taskFactory.getBlockCleanerTask();
        nodesCleanupTask = taskFactory.getNodesCleanupTask();
        syncSnapshotTask = taskFactory.getSyncSnapshotTask();

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

        syncSnapshotService = new ISnapshotSynchronizationService() {
            private SyncSnapshotService inner = peerServiceFactory.getSyncSnapshotService();

            @Override
            public Block getLastBlock() throws RemotePeerException, IOException {
                Block value = inner.getLastBlock();
                String valueStr = mapper.writeValueAsString(value);

                return mapper.readValue(valueStr, Block.class);
            }

            @Override
            public Block getBlockByHeight(int height) throws RemotePeerException, IOException {
                Block value = inner.getBlockByHeight(height);
                String valueStr = mapper.writeValueAsString(value);

                return mapper.readValue(valueStr, Block.class);
            }

            @Override
            public Block[] getBlocksHeadFrom(int height) throws RemotePeerException, IOException {
                Block[] value = inner.getBlocksHeadFrom(height);
                String valueStr = mapper.writeValueAsString(value);

                return mapper.readValue(valueStr, Block[].class);
            }

            @Override
            public Map<String, Object> getAccounts(String blockID) throws RemotePeerException, IOException {
                Map<String, Object> value = inner.getAccounts(blockID);
                String valueStr = mapper.writeValueAsString(value);

                return mapper.readValue(valueStr, Map.class);
            }

            @Override
            public Map<String, Object> getNextAccounts(String blockID,
                                                       String accountID) throws RemotePeerException, IOException {
                Map<String, Object> value = inner.getNextAccounts(blockID, accountID);
                String valueStr = mapper.writeValueAsString(value);

                return mapper.readValue(valueStr, Map.class);
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
                if (clazz.equals(ISnapshotSynchronizationService.class)) {
                    return (TService) ctx.syncSnapshotService;
                }

                return null;
            }
        });
    }

    public ISigner getSigner() {
        return signer;
    }
}
