package com.exscudo.eon.cfg;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.eon.jsonrpc.JrpcServiceProxyFactory;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.backlog.Backlog;
import com.exscudo.peer.core.backlog.BacklogCleaner;
import com.exscudo.peer.core.blockchain.BlockchainProvider;
import com.exscudo.peer.core.blockchain.TransactionProvider;
import com.exscudo.peer.core.blockchain.events.BlockEventManager;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.crypto.ed25519.Ed25519SignatureVerifier;
import com.exscudo.peer.core.crypto.ed25519.Ed25519Signer;
import com.exscudo.peer.core.crypto.mapper.SignedObjectMapper;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.importer.BlockGenerator;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.eon.ForkInitializer;

public class PeerStarter {

    private final Config config;
    private ExecutionContext executionContext = null;
    private Storage storage = null;
    private TimeProvider timeProvider = null;
    private Backlog backlog = null;
    private BlockchainProvider blockchainProvider = null;
    private TransactionProvider transactionProvider = null;

    private IFork fork = null;
    private JrpcServiceProxyFactory proxyFactory = null;
    private ISigner signer = null;
    private BlockGenerator blockGenerator = null;
    private BacklogCleaner cleaner = null;
    private Ed25519SignatureVerifier signatureVerifier = null;
    private CryptoProvider cryptoProvider = null;
    private LedgerProvider ledgerProvider = null;
    private BlockEventManager blockEventManager = null;

    public PeerStarter(Config config) {
        this.config = config;
    }

    public ExecutionContext getExecutionContext() throws SQLException, IOException, ClassNotFoundException {
        if (executionContext == null) {
            ExecutionContext context = new ExecutionContext();
            context.setVersion("0.9.0");
            context.setApplication("EON");
            context.setHost(new ExecutionContext.Host(this.config.getHost()));
            context.setBlacklistingPeriod(this.config.getBlacklistingPeriod());

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
        if (storage == null) {
            Storage storage =
                    Storage.create(this.config.getDbUrl(), this.config.getGenesisFile(), this.config.isFullSync());
            setStorage(storage);
        }
        return storage;
    }

    public void setStorage(Storage storage) {
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
            setBacklog(new Backlog(getFork(),
                                   getBlockchainProvider(),
                                   getLedgerProvider(),
                                   getTransactionProvider(),
                                   getTimeProvider()));
        }
        return backlog;
    }

    public void setBacklog(Backlog backlog) {
        this.backlog = backlog;
    }

    public BlockchainProvider getBlockchainProvider() throws SQLException, IOException, ClassNotFoundException {
        if (blockchainProvider == null) {
            setBlockchainProvider(new BlockchainProvider(getStorage(), getFork(), getBlockEventManager()));
        }
        return blockchainProvider;
    }

    public void setBlockchainProvider(BlockchainProvider blockchainProvider) {
        this.blockchainProvider = blockchainProvider;
    }

    public TransactionProvider getTransactionProvider() throws SQLException, IOException, ClassNotFoundException {
        if (transactionProvider == null) {
            setTransactionProvider(new TransactionProvider(getStorage()));
        }
        return transactionProvider;
    }

    public void setTransactionProvider(TransactionProvider transactionProvider) {
        this.transactionProvider = transactionProvider;
    }

    public IFork getFork() throws SQLException, IOException, ClassNotFoundException {
        if (fork == null) {
            setFork(ForkInitializer.init(getStorage()));
        }
        return fork;
    }

    public void setFork(IFork fork) {
        this.fork = fork;
    }

    public JrpcServiceProxyFactory getProxyFactory() {
        if (proxyFactory == null) {
            Map<String, String> clazzMap = new HashMap<>();
            Map<String, String> clazzMapImpl = new HashMap<>();

            clazzMap.put("com.exscudo.peer.core.api.IMetadataService", "metadata");
            clazzMapImpl.put("com.exscudo.peer.core.api.IMetadataService",
                             "com.exscudo.eon.jsonrpc.proxy.MetadataServiceProxy");

            clazzMap.put("com.exscudo.peer.core.api.ITransactionSynchronizationService", "transactions");
            clazzMapImpl.put("com.exscudo.peer.core.api.ITransactionSynchronizationService",
                             "com.exscudo.eon.jsonrpc.proxy.TransactionSynchronizationServiceProxy");

            clazzMap.put("com.exscudo.peer.core.api.IBlockSynchronizationService", "blocks");
            clazzMapImpl.put("com.exscudo.peer.core.api.IBlockSynchronizationService",
                             "com.exscudo.eon.jsonrpc.proxy.BlockSynchronizationServiceProxy");

            clazzMap.put("com.exscudo.peer.core.api.ISnapshotSynchronizationService", "snapshot");
            clazzMapImpl.put("com.exscudo.peer.core.api.ISnapshotSynchronizationService",
                             "com.exscudo.eon.jsonrpc.proxy.SnapshotSynchronizationServiceProxy");

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
            setSigner(Ed25519Signer.createNew(this.config.getSeed()));
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
            setCleaner(new BacklogCleaner(getBacklog(), getStorage()));
        }
        return cleaner;
    }

    public void setCleaner(BacklogCleaner cleaner) throws SQLException, IOException, ClassNotFoundException {
        this.cleaner = cleaner;
    }

    public Ed25519SignatureVerifier getSignatureVerifier() {
        if (signatureVerifier == null) {
            setSignatureVerifier(new Ed25519SignatureVerifier());
        }
        return signatureVerifier;
    }

    public void setSignatureVerifier(Ed25519SignatureVerifier signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }

    public CryptoProvider getCryptoProvider() throws SQLException, IOException, ClassNotFoundException {
        if (cryptoProvider == null) {

            CryptoProvider provider = new CryptoProvider(new SignedObjectMapper(getFork().getGenesisBlockID()));
            provider.addProvider(getSignatureVerifier());
            provider.setDefaultProvider(getSignatureVerifier().getName());

            setCryptoProvider(provider);
        }
        return cryptoProvider;
    }

    public void setCryptoProvider(CryptoProvider cryptoProvider) {
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

    public BlockEventManager getBlockEventManager() throws SQLException, IOException, ClassNotFoundException {
        if (blockEventManager == null) {
            BlockEventManager manager = new BlockEventManager();
            setBlockEventManager(manager);
        }
        return blockEventManager;
    }

    public void setBlockEventManager(BlockEventManager blockEventManager) {
        this.blockEventManager = blockEventManager;
    }
}
