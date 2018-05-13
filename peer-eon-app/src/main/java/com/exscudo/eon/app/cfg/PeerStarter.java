package com.exscudo.eon.app.cfg;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.exscudo.eon.app.jsonrpc.JrpcServiceProxyFactory;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.backlog.Backlog;
import com.exscudo.peer.core.backlog.BacklogCleaner;
import com.exscudo.peer.core.backlog.events.BacklogEventManager;
import com.exscudo.peer.core.blockchain.Blockchain;
import com.exscudo.peer.core.blockchain.BlockchainProvider;
import com.exscudo.peer.core.blockchain.events.BlockchainEventManager;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.Signer;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.importer.BlockGenerator;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.core.middleware.TransactionParser;
import com.exscudo.peer.core.storage.Initializer;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.eon.midleware.CompositeTransactionParser;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinPaymentParser;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinRegistrationParser;
import com.exscudo.peer.eon.midleware.parsers.ColoredCoinSupplyParser;
import com.exscudo.peer.eon.midleware.parsers.ComplexPaymentParser;
import com.exscudo.peer.eon.midleware.parsers.DelegateParser;
import com.exscudo.peer.eon.midleware.parsers.DepositParser;
import com.exscudo.peer.eon.midleware.parsers.PaymentParser;
import com.exscudo.peer.eon.midleware.parsers.PublicationParser;
import com.exscudo.peer.eon.midleware.parsers.QuorumParser;
import com.exscudo.peer.eon.midleware.parsers.RegistrationParser;
import com.exscudo.peer.eon.midleware.parsers.RejectionParser;
import com.exscudo.peer.tx.TransactionType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PeerStarter {

    private final Config config;
    private ExecutionContext executionContext = null;
    private Storage storage = null;
    private TimeProvider timeProvider = null;
    private Backlog backlog = null;
    private BlockchainProvider blockchainProvider = null;

    private IFork fork = null;
    private JrpcServiceProxyFactory proxyFactory = null;
    private ISigner signer = null;
    private BlockGenerator blockGenerator = null;
    private BacklogCleaner cleaner = null;
    private CryptoProvider cryptoProvider = null;
    private LedgerProvider ledgerProvider = null;
    private BlockchainEventManager blockchainEventManager = null;
    private BacklogEventManager backlogEventManager = null;

    public PeerStarter(Config config) throws IOException, SQLException, ClassNotFoundException {
        this.config = config;

        TransactionParser.init(CompositeTransactionParser.create()
                                                         .addParser(TransactionType.Registration,
                                                                    new RegistrationParser())
                                                         .addParser(TransactionType.Payment, new PaymentParser())
                                                         .addParser(TransactionType.Deposit, new DepositParser())
                                                         .addParser(TransactionType.Delegate, new DelegateParser())
                                                         .addParser(TransactionType.Quorum, new QuorumParser())
                                                         .addParser(TransactionType.Rejection, new RejectionParser())
                                                         .addParser(TransactionType.Publication,
                                                                    new PublicationParser())
                                                         .addParser(TransactionType.ColoredCoinRegistration,
                                                                    new ColoredCoinRegistrationParser())
                                                         .addParser(TransactionType.ColoredCoinPayment,
                                                                    new ColoredCoinPaymentParser())
                                                         .addParser(TransactionType.ColoredCoinSupply,
                                                                    new ColoredCoinSupplyParser())
                                                         .addParser(TransactionType.ComplexPayment,
                                                                    new ComplexPaymentParser(CompositeTransactionParser.create()
                                                                                                                       .addParser(
                                                                                                                               TransactionType.Payment,
                                                                                                                               new PaymentParser())
                                                                                                                       .addParser(
                                                                                                                               TransactionType.ColoredCoinPayment,
                                                                                                                               new ColoredCoinPaymentParser())
                                                                                                                       .build()))
                                                         .build());

        initialize();
    }

    private void initialize() throws IOException, SQLException, ClassNotFoundException {
        ObjectMapper objectMapper = new ObjectMapper();

        URI uri;
        try {
            uri = this.getClass().getClassLoader().getResource(config.getGenesisFile()).toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        byte[] json = Files.readAllBytes(Paths.get(uri));
        Map<String, Object> genesisBlock = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });

        byte[] signature = Format.convert(genesisBlock.get("signature").toString());
        int timestamp = Integer.parseInt(genesisBlock.get("timestamp").toString());
        BlockID networkID = new BlockID(signature, timestamp);

        Storage storage = Storage.create(config.getDbUrl());
        try {
            String id = storage.metadata().getProperty("GENESIS_BLOCK_ID");
            if (id != null) {
                BlockID genesisBlockID = new BlockID(Long.parseLong(id));
                if (!networkID.equals(genesisBlockID)) {
                    throw new UnsupportedOperationException("Genesis Block ID dose not match with configuration.");
                }
            }
        } catch (SQLException ignore) {

        }
        Initializer initializer = new Initializer();
        initializer.initialize(storage);

        String id = storage.metadata().getProperty("GENESIS_BLOCK_ID");
        if (id == null) {
            loadGenesis(storage, genesisBlock);
            id = storage.metadata().getProperty("GENESIS_BLOCK_ID");
            if (!networkID.equals(new BlockID(Long.parseLong(id)))) {
                throw new UnsupportedOperationException("Data corrupted.");
            }
        }
        checkSynchronizingMode(storage);
        setStorage(storage);
    }

    private ForkProperties parseForkProperties() throws IOException {

        URI uri;
        try {
            uri = this.getClass().getClassLoader().getResource(config.getForksFile()).toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] json = Files.readAllBytes(Paths.get(uri));
        Map<String, Object> forksMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });

        ForkProperties props = new ForkProperties();
        props.setMinDepositSize(Long.parseLong(forksMap.get("min_deposit_size").toString()));
        props.setDateEndAll(forksMap.get("date_end_all").toString());

        List<ForkProperties.Period> forksPeriods = new LinkedList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> periodJson = (List<Map<String, Object>>) forksMap.get("forks");
        for (Map<String, Object> map : periodJson) {

            ForkProperties.Period p = new ForkProperties.Period();

            p.setNumber((Integer) map.get("number"));
            p.setDateBegin(map.get("date_begin").toString());

            if (map.containsKey("add_tx_types")) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) map.get("add_tx_types");
                p.setAddedTxTypes(list.toArray(new String[0]));
            }
            if (map.containsKey("remove_tx_types")) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) map.get("remove_tx_types");
                p.setRemovedTxTypes(list.toArray(new String[0]));
            }

            forksPeriods.add(p);

            int size = 2;
            size += map.containsKey("add_tx_types") ? 1 : 0;
            size += map.containsKey("remove_tx_types") ? 1 : 0;

            if (size != map.size()) {
                throw new IOException("Invalid forks-file format");
            }
        }

        props.setPeriods(forksPeriods.toArray(new ForkProperties.Period[0]));

        return props;
    }

    private void checkSynchronizingMode(Storage storage) throws SQLException {
        boolean fullSync = config.isFullSync();
        Storage.Metadata metadata = storage.metadata();
        String full = metadata.getProperty("FULL");
        if (full == null) {
            if (fullSync) {
                metadata.setProperty("FULL", "1");
                metadata.setHistoryFromHeight(0);
            } else {
                metadata.setProperty("FULL", "0");
            }

            return;
        }

        if (fullSync && full.equals("0")) {
            throw new RuntimeException("Incorrect config - fullSync enabled - DB must be cleaned");
        }
        if (!fullSync && full.equals("1")) {
            metadata.setProperty("FULL", "0");
        }
    }

    private void loadGenesis(Storage storage, Map<String, Object> map) throws IOException, SQLException {

        UnsafeBlockchain blockchain = new UnsafeBlockchain(storage);

        Block block = new Block();
        block.setVersion(-1);
        block.setTimestamp(Integer.parseInt(map.get("timestamp").toString()));
        block.setPreviousBlock(new BlockID(0));
        block.setSenderID(new AccountID(0));
        block.setGenerationSignature(new byte[64]);
        block.setSignature(Format.convert(map.get("signature").toString()));
        block.setHeight(0);
        block.setCumulativeDifficulty(BigInteger.ZERO);
        block.setTransactions(new ArrayList<>());

        Map<String, Object> accSetMap = (Map<String, Object>) map.get("accounts");
        LedgerProvider ledgerProvider = new LedgerProvider(storage);
        ILedger ledger = ledgerProvider.getLedger(block);

        for (String id : accSetMap.keySet()) {
            Map<String, Object> accMap = (Map<String, Object>) accSetMap.get(id);
            Account acc = new Account(new AccountID(id));
            for (String p : accMap.keySet()) {
                Map<String, Object> data = (Map<String, Object>) accMap.get(p);
                AccountProperty property = new AccountProperty(p, data);
                acc = acc.putProperty(property);
            }

            ledger = ledger.putAccount(acc);
        }

        block.setSnapshot(ledger.getHash());

        ledgerProvider.addLedger(ledger);

        blockchain.addBlock(block);
        blockchain.save();
        storage.metadata().setProperty("GENESIS_BLOCK_ID", Long.toString(block.getID().getValue()));
    }

    public ExecutionContext getExecutionContext() throws SQLException, IOException, ClassNotFoundException {
        if (executionContext == null) {

            ExecutionContext context = new ExecutionContext();
            context.setVersion(config.getVersion());
            context.setApplication("EON");
            context.setHost(new ExecutionContext.Host(this.config.getHost()));
            context.setBlacklistingPeriod(this.config.getBlacklistingPeriod());
            context.setConnectedPoolSize(this.config.getConnectedPoolSize());

            for (String address : this.config.getInnerPeers()) {
                address = address.trim();
                if (address.length() > 0) {
                    context.addInnerPeer(address);
                }
            }

            for (String address : this.config.getPublicPeers()) {
                address = address.trim();
                if (address.length() > 0) {
                    context.addImmutablePeer(address);
                }
            }

            context.setProxyFactory(getProxyFactory());

            setExecutionContext(context);
        }
        return executionContext;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public Storage getStorage() throws SQLException, IOException, ClassNotFoundException {
        return storage;
    }

    private void setStorage(Storage storage) {
        this.storage = storage;
    }

    public TimeProvider getTimeProvider() {
        if (timeProvider == null) {
            setTimeProvider(new TimeProvider());
        }
        return timeProvider;
    }

    public void setTimeProvider(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public Backlog getBacklog() throws SQLException, IOException, ClassNotFoundException {
        if (backlog == null) {
            setBacklog(new Backlog(getBacklogEventManager(),
                                   getFork(),
                                   getStorage(),
                                   getBlockchainProvider(),
                                   getLedgerProvider(),
                                   getTimeProvider()));
        }
        return backlog;
    }

    public void setBacklog(Backlog backlog) {
        this.backlog = backlog;
    }

    public BlockchainProvider getBlockchainProvider() throws SQLException, IOException, ClassNotFoundException {
        if (blockchainProvider == null) {
            setBlockchainProvider(new BlockchainProvider(getStorage(), getBlockchainEventManager()));
        }
        return blockchainProvider;
    }

    public void setBlockchainProvider(BlockchainProvider blockchainProvider) {
        this.blockchainProvider = blockchainProvider;
    }

    public IFork getFork() throws SQLException, IOException, ClassNotFoundException {
        if (fork == null) {
            ForkProperties forkProps = parseForkProperties();
            setFork(ForkInitializer.init(getStorage().metadata().getGenesisBlockID(), forkProps));
        }
        return fork;
    }

    public void setFork(IFork fork) {
        this.fork = fork;
    }

    public JrpcServiceProxyFactory getProxyFactory() throws SQLException, IOException, ClassNotFoundException {
        if (proxyFactory == null) {
            Map<String, String> clazzMap = new HashMap<>();
            Map<String, String> clazzMapImpl = new HashMap<>();

            clazzMap.put("com.exscudo.peer.core.api.IMetadataService", "metadata");
            clazzMapImpl.put("com.exscudo.peer.core.api.IMetadataService",
                             "com.exscudo.eon.app.jsonrpc.proxy.MetadataServiceProxy");

            clazzMap.put("com.exscudo.peer.core.api.ITransactionSynchronizationService", "transactions");
            clazzMapImpl.put("com.exscudo.peer.core.api.ITransactionSynchronizationService",
                             "com.exscudo.eon.app.jsonrpc.proxy.TransactionSynchronizationServiceProxy");

            clazzMap.put("com.exscudo.peer.core.api.IBlockSynchronizationService", "blocks");
            clazzMapImpl.put("com.exscudo.peer.core.api.IBlockSynchronizationService",
                             "com.exscudo.eon.app.jsonrpc.proxy.BlockSynchronizationServiceProxy");

            clazzMap.put("com.exscudo.peer.core.api.ISnapshotSynchronizationService", "snapshot");
            clazzMapImpl.put("com.exscudo.peer.core.api.ISnapshotSynchronizationService",
                             "com.exscudo.eon.app.jsonrpc.proxy.SnapshotSynchronizationServiceProxy");

            JrpcServiceProxyFactory factory = new JrpcServiceProxyFactory(clazzMap, clazzMapImpl);

            factory.setConnectTimeout(this.config.getConnectTimeout());
            factory.setReadTimeout(this.config.getReadTimeout());

            setProxyFactory(factory);
        }
        return proxyFactory;
    }

    public void setProxyFactory(JrpcServiceProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public ISigner getSigner() {
        if (signer == null) {
            setSigner(Signer.createNew(this.config.getSeed()));
        }
        return signer;
    }

    public void setSigner(ISigner signer) {
        this.signer = signer;
    }

    public BlockGenerator getBlockGenerator() throws SQLException, IOException, ClassNotFoundException {
        if (blockGenerator == null) {
            setBlockGenerator(new BlockGenerator(getFork(),
                                                 getSigner(),
                                                 getBacklog(),
                                                 getBlockchainProvider(),
                                                 getLedgerProvider()));
        }
        return blockGenerator;
    }

    public void setBlockGenerator(BlockGenerator blockGenerator) throws SQLException, IOException, ClassNotFoundException {
        this.blockGenerator = blockGenerator;
    }

    public BacklogCleaner getCleaner() throws SQLException, IOException, ClassNotFoundException {
        if (cleaner == null) {
            setCleaner(new BacklogCleaner(getBacklog()));
        }
        return cleaner;
    }

    public void setCleaner(BacklogCleaner cleaner) throws SQLException, IOException, ClassNotFoundException {
        this.cleaner = cleaner;
    }

    public CryptoProvider getSignatureVerifier() throws SQLException, IOException, ClassNotFoundException {
        if (cryptoProvider == null) {
            setSignatureVerifier(CryptoProvider.getInstance());
        }
        return cryptoProvider;
    }

    public void setSignatureVerifier(CryptoProvider cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
    }

    public LedgerProvider getLedgerProvider() throws SQLException, IOException, ClassNotFoundException {
        if (ledgerProvider == null) {
            setLedgerProvider(new LedgerProvider(getStorage()));
        }
        return ledgerProvider;
    }

    public void setLedgerProvider(LedgerProvider ledgerProvider) {
        this.ledgerProvider = ledgerProvider;
    }

    public BlockchainEventManager getBlockchainEventManager() throws SQLException, IOException, ClassNotFoundException {
        if (blockchainEventManager == null) {
            BlockchainEventManager manager = new BlockchainEventManager();
            setBlockchainEventManager(manager);
        }
        return blockchainEventManager;
    }

    public void setBlockchainEventManager(BlockchainEventManager blockchainEventManager) {
        this.blockchainEventManager = blockchainEventManager;
    }

    public BacklogEventManager getBacklogEventManager() {
        if (backlogEventManager == null) {
            BacklogEventManager manager = new BacklogEventManager();
            setBacklogEventManager(manager);
        }
        return backlogEventManager;
    }

    public void setBacklogEventManager(BacklogEventManager backlogEventManager) {
        this.backlogEventManager = backlogEventManager;
    }

    public Config getConfig() {
        return config;
    }

    private static class UnsafeBlockchain extends Blockchain {

        private static final Block ZERO_BLOCK = new Block() {
            {
                setVersion(-1);
                setTimestamp(0);
                setPreviousBlock(new BlockID(0));
                setSenderID(new AccountID(0));
                setGenerationSignature(new byte[0]);
                setSignature(new byte[0]);
                setHeight(-1);
                setCumulativeDifficulty(BigInteger.ZERO);
                setTransactions(new ArrayList<>());
                setSnapshot("");
            }

            @Override
            public BlockID getID() {
                return new BlockID(0);
            }
        };

        private final Storage storage;

        public UnsafeBlockchain(Storage storage) {
            super(storage, ZERO_BLOCK);

            addBlock(ZERO_BLOCK);
            this.storage = storage;
        }

        public void save() throws SQLException {

            BlockchainProvider mockService = new BlockchainProvider(storage, new BlockchainEventManager()) {

                @Override
                public void initialize() {
                }

                @Override
                public Block getLastBlock() {
                    return ZERO_BLOCK;
                }

                @Override
                public Block setLastBlock(Block newLastBlock) {
                    super.setPointerTo(newLastBlock);
                    return newLastBlock;
                }
            };

            mockService.setBlockchain(this);
        }
    }
}
