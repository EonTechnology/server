package com.exscudo.eon.app.IT;

import java.io.IOException;
import java.sql.SQLException;

import com.exscudo.eon.app.api.bot.AccountBotService;
import com.exscudo.eon.app.api.bot.ColoredCoinBotService;
import com.exscudo.eon.app.api.bot.TransactionBotService;
import com.exscudo.eon.app.api.peer.SyncBlockService;
import com.exscudo.eon.app.api.peer.SyncMetadataService;
import com.exscudo.eon.app.api.peer.SyncSnapshotService;
import com.exscudo.eon.app.api.peer.SyncTransactionService;
import com.exscudo.eon.app.cfg.BotServiceFactory;
import com.exscudo.eon.app.cfg.PeerServiceFactory;
import com.exscudo.eon.app.cfg.PeerStarter;
import com.exscudo.eon.app.cfg.TaskFactory;
import com.exscudo.eon.app.jsonrpc.ObjectMapperProvider;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.api.IBlockSynchronizationService;
import com.exscudo.peer.core.api.IMetadataService;
import com.exscudo.peer.core.api.ISnapshotSynchronizationService;
import com.exscudo.peer.core.api.ITransactionSynchronizationService;
import com.exscudo.peer.core.api.SalientAttributes;
import com.exscudo.peer.core.backlog.Backlog;
import com.exscudo.peer.core.backlog.BacklogCleaner;
import com.exscudo.peer.core.backlog.services.BacklogService;
import com.exscudo.peer.core.backlog.tasks.SyncForkedTransactionListTask;
import com.exscudo.peer.core.backlog.tasks.SyncTransactionListTask;
import com.exscudo.peer.core.blockchain.IBlockchainProvider;
import com.exscudo.peer.core.blockchain.services.BlockService;
import com.exscudo.peer.core.blockchain.services.TransactionService;
import com.exscudo.peer.core.blockchain.tasks.BlockCleanerTask;
import com.exscudo.peer.core.blockchain.tasks.NestedTransactionCleanupTask;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Account;
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

class PeerContext {

    BlockGenerator generator;
    Backlog backlog;
    IBlockchainProvider blockchain;
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
    NestedTransactionCleanupTask nestedTransactionCleanupTask;

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

    PeerContext(PeerStarter starter) throws SQLException, IOException, ClassNotFoundException {

        // Init context
        this.context = starter.getExecutionContext();

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
        this.ledgerProvider = starter.getLedgerProvider();
        this.backlogCleaner = starter.getCleaner();

        // Init listeners
        starter.getBlockchainEventManager().addListener(starter.getCleaner());

        // Init bot services
        accountBotService = botServiceFactory.getAccountBotService();
        transactionBotService = new TransactionBotService(starter.getBacklog()) {
            @Override
            public void putTransaction(Transaction tx) throws RemotePeerException, IOException {
                ObjectMapper mapper = ObjectMapperProvider.createMapper();
                String valueStr = mapper.writeValueAsString(tx);
                Loggers.debug(TransactionBotService.class, "putTransaction: " + valueStr);

                super.putTransaction(mapper.readValue(valueStr, Transaction.class));
            }
        };

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
        nestedTransactionCleanupTask = taskFactory.getNestedTransactionCleanupTask();

        syncTransactionPeerService = new ITransactionSynchronizationService() {

            private SyncTransactionService inner = peerServiceFactory.getSyncTransactionService();

            @Override
            public Transaction[] getTransactions(String lastBlockId,
                                                 String[] ignoreList) throws RemotePeerException, IOException {
                ObjectMapper mapper = ObjectMapperProvider.createMapper();
                Transaction[] value = inner.getTransactions(lastBlockId, ignoreList);
                String valueStr = mapper.writeValueAsString(value);

                Loggers.debug(ITransactionSynchronizationService.class, "getTransactions: " + valueStr);

                return mapper.readValue(valueStr, Transaction[].class);
            }
        };

        syncBlockPeerService = new IBlockSynchronizationService() {

            private SyncBlockService inner = peerServiceFactory.getSyncBlockService();

            @Override
            public Difficulty getDifficulty() throws RemotePeerException, IOException {
                ObjectMapper mapper = ObjectMapperProvider.createMapper();
                Difficulty value = inner.getDifficulty();
                String valueStr = mapper.writeValueAsString(value);

                Loggers.debug(IBlockSynchronizationService.class, "getDifficulty: " + valueStr);

                return mapper.readValue(valueStr, Difficulty.class);
            }

            @Override
            public Block[] getBlockHistory(String[] blockSequence) throws RemotePeerException, IOException {
                ObjectMapper mapper = ObjectMapperProvider.createMapper();
                Block[] value = inner.getBlockHistory(blockSequence);
                String valueStr = mapper.writeValueAsString(value);

                Loggers.debug(IBlockSynchronizationService.class, "getBlockHistory: " + valueStr);

                return mapper.readValue(valueStr, Block[].class);
            }

            @Override
            public Block getLastBlock() throws RemotePeerException, IOException {
                ObjectMapper mapper = ObjectMapperProvider.createMapper();
                Block value = inner.getLastBlock();
                String valueStr = mapper.writeValueAsString(value);

                Loggers.debug(IBlockSynchronizationService.class, "getLastBlock: " + valueStr);

                return mapper.readValue(valueStr, Block.class);
            }
        };
        syncMetadataPeerService = new IMetadataService() {
            private SyncMetadataService inner = peerServiceFactory.getSyncMetadataService();

            @Override
            public SalientAttributes getAttributes() throws RemotePeerException, IOException {
                ObjectMapper mapper = ObjectMapperProvider.createMapper();
                SalientAttributes value = inner.getAttributes();
                String valueStr = mapper.writeValueAsString(value);

                Loggers.debug(IMetadataService.class, "getAttributes: " + valueStr);

                return mapper.readValue(valueStr, SalientAttributes.class);
            }

            @Override
            public String[] getWellKnownNodes() throws RemotePeerException, IOException {
                ObjectMapper mapper = ObjectMapperProvider.createMapper();
                String[] value = inner.getWellKnownNodes();
                String valueStr = mapper.writeValueAsString(value);

                Loggers.debug(IMetadataService.class, "getWellKnownNodes: " + valueStr);

                return mapper.readValue(valueStr, String[].class);
            }

            @Override
            public boolean addPeer(long peerID, String address) throws RemotePeerException, IOException {
                Loggers.debug(IMetadataService.class, "addPeer: ");
                return inner.addPeer(peerID, address);
            }
        };

        syncSnapshotService = new ISnapshotSynchronizationService() {
            private SyncSnapshotService inner = peerServiceFactory.getSyncSnapshotService();

            @Override
            public Block getLastBlock() throws RemotePeerException, IOException {
                ObjectMapper mapper = ObjectMapperProvider.createMapper();
                Block value = inner.getLastBlock();
                String valueStr = mapper.writeValueAsString(value);

                Loggers.debug(ISnapshotSynchronizationService.class, "getLastBlock: " + valueStr);

                return mapper.readValue(valueStr, Block.class);
            }

            @Override
            public Block getBlockByHeight(int height) throws RemotePeerException, IOException {
                ObjectMapper mapper = ObjectMapperProvider.createMapper();
                Block value = inner.getBlockByHeight(height);
                String valueStr = mapper.writeValueAsString(value);

                Loggers.debug(ISnapshotSynchronizationService.class, "getBlockByHeight: " + valueStr);

                return mapper.readValue(valueStr, Block.class);
            }

            @Override
            public Block[] getBlocksHeadFrom(int height) throws RemotePeerException, IOException {
                ObjectMapper mapper = ObjectMapperProvider.createMapper();
                Block[] value = inner.getBlocksHeadFrom(height);
                String valueStr = mapper.writeValueAsString(value);

                Loggers.debug(ISnapshotSynchronizationService.class, "getBlocksHeadFrom: " + valueStr);

                return mapper.readValue(valueStr, Block[].class);
            }

            @Override
            public Account[] getAccounts(String blockID) throws RemotePeerException, IOException {
                ObjectMapper mapper = ObjectMapperProvider.createMapper();
                Account[] accounts = inner.getAccounts(blockID);
                String valueStr = mapper.writeValueAsString(accounts);

                Loggers.debug(ISnapshotSynchronizationService.class, "getAccounts: " + valueStr);

                return mapper.readValue(valueStr, Account[].class);
            }

            @Override
            public Account[] getNextAccounts(String blockID, String accountID) throws RemotePeerException, IOException {
                ObjectMapper mapper = ObjectMapperProvider.createMapper();
                Account[] accounts = inner.getNextAccounts(blockID, accountID);
                String valueStr = mapper.writeValueAsString(accounts);

                Loggers.debug(ISnapshotSynchronizationService.class, "getNextAccounts: " + valueStr);

                return mapper.readValue(valueStr, Account[].class);
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

    public BlockID getNetworkID() {
        return fork.getGenesisBlockID();
    }
}
