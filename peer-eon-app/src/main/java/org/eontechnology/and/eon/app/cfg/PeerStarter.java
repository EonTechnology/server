package org.eontechnology.and.eon.app.cfg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.eontechnology.and.eon.app.jsonrpc.JrpcServiceProxyFactory;
import org.eontechnology.and.eon.app.utils.TransactionEstimator;
import org.eontechnology.and.peer.core.backlog.Backlog;
import org.eontechnology.and.peer.core.backlog.BacklogCleaner;
import org.eontechnology.and.peer.core.backlog.events.BacklogEventManager;
import org.eontechnology.and.peer.core.blockchain.Blockchain;
import org.eontechnology.and.peer.core.blockchain.BlockchainProvider;
import org.eontechnology.and.peer.core.blockchain.ITransactionMapper;
import org.eontechnology.and.peer.core.blockchain.TransactionMapper;
import org.eontechnology.and.peer.core.blockchain.events.BlockchainEventManager;
import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.common.IAccountHelper;
import org.eontechnology.and.peer.core.common.ITimeProvider;
import org.eontechnology.and.peer.core.common.ITransactionEstimator;
import org.eontechnology.and.peer.core.common.TimeProvider;
import org.eontechnology.and.peer.core.crypto.CryptoProvider;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.crypto.Signer;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.AccountProperty;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.env.ExecutionContext;
import org.eontechnology.and.peer.core.importer.BlockGenerator;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.ledger.LedgerProvider;
import org.eontechnology.and.peer.core.storage.Initializer;
import org.eontechnology.and.peer.core.storage.Storage;

public class PeerStarter {

  private static final Block ZERO_BLOCK =
      new Block() {
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
  private final Config config;
  private ExecutionContext executionContext = null;
  private Storage storage = null;
  private ITimeProvider timeProvider = null;
  private Backlog backlog = null;
  private BlockchainProvider blockchainProvider = null;
  private Fork fork = null;
  private JrpcServiceProxyFactory proxyFactory = null;
  private ISigner signer = null;
  private BlockGenerator blockGenerator = null;
  private BacklogCleaner cleaner = null;
  private CryptoProvider cryptoProvider = null;
  private LedgerProvider ledgerProvider = null;
  private BlockchainEventManager blockchainEventManager = null;
  private BacklogEventManager backlogEventManager = null;
  private TransactionValidatorFabricImpl transactionValidatorFabric = null;
  private ITransactionEstimator estimator = null;
  private IAccountHelper accountHelper = null;
  private ITransactionMapper transactionMapper;
  private BlockID networkID;
  private int genesisTimestamp;

  public PeerStarter(Config config) throws IOException, SQLException, ClassNotFoundException {
    this.config = config;
  }

  public static PeerStarter create(Config config)
      throws SQLException, IOException, ClassNotFoundException {
    PeerStarter peerStarter = new PeerStarter(config);
    peerStarter.initialize();
    return peerStarter;
  }

  public void initialize() throws IOException, SQLException, ClassNotFoundException {
    ObjectMapper objectMapper = new ObjectMapper();

    URI uri;
    try {
      uri = this.getClass().getClassLoader().getResource(config.getGenesisFile()).toURI();
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
    byte[] json = Files.readAllBytes(Paths.get(uri));
    Map<String, Object> genesisBlock =
        objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

    byte[] signature = Format.convert(genesisBlock.get("signature").toString());
    int timestamp = Integer.parseInt(genesisBlock.get("timestamp").toString());
    networkID = new BlockID(signature, timestamp);
    genesisTimestamp = timestamp;

    Storage storage = Storage.create(config.getDbUrl());
    try {
      String id = storage.metadata().getProperty("GENESIS_BLOCK_ID");
      if (id != null) {
        BlockID genesisBlockID = new BlockID(Long.parseLong(id));
        if (!networkID.equals(genesisBlockID)) {
          throw new UnsupportedOperationException(
              "Genesis Block ID dose not match with configuration.");
        }
      }
    } catch (SQLException ignore) {

    }
    Initializer initializer = new Initializer(getFork());
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

  private void loadGenesis(Storage storage, Map<String, Object> map)
      throws IOException, SQLException, ClassNotFoundException {

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
    LedgerProvider ledgerProvider = new LedgerProvider(storage, getFork());
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

    BlockchainProvider blockchainProvider = new BlockchainProvider(storage, null);
    Blockchain blockchain = blockchainProvider.createBlockchain();
    blockchain.addBlock(ZERO_BLOCK);
    blockchain.addBlock(block);
    blockchainProvider.initialize(blockchain, block.getID(), -1);
  }

  public ExecutionContext getExecutionContext()
      throws SQLException, IOException, ClassNotFoundException {
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

  public ITimeProvider getTimeProvider() {
    if (timeProvider == null) {
      setTimeProvider(new TimeProvider());
    }
    return timeProvider;
  }

  public void setTimeProvider(ITimeProvider timeProvider) {
    this.timeProvider = timeProvider;
  }

  public Backlog getBacklog() throws SQLException, IOException, ClassNotFoundException {
    if (backlog == null) {
      setBacklog(
          new Backlog(
              getFork(),
              getBacklogEventManager(),
              getStorage(),
              getBlockchainProvider(),
              getLedgerProvider(),
              getTimeProvider(),
              getTransactionValidatorFabric(),
              getEstimator()));
    }
    return backlog;
  }

  public void setBacklog(Backlog backlog) {
    this.backlog = backlog;
  }

  public BlockchainProvider getBlockchainProvider()
      throws SQLException, IOException, ClassNotFoundException {
    if (blockchainProvider == null) {
      BlockchainProvider bp = new BlockchainProvider(getStorage(), getBlockchainEventManager());
      bp.initialize();
      setBlockchainProvider(bp);
    }
    return blockchainProvider;
  }

  public void setBlockchainProvider(BlockchainProvider blockchainProvider) {
    this.blockchainProvider = blockchainProvider;
  }

  public Fork getFork() throws SQLException, IOException, ClassNotFoundException {
    if (fork == null) {
      URI uri;
      try {
        uri = this.getClass().getClassLoader().getResource(config.getForksFile()).toURI();
      } catch (URISyntaxException e) {
        throw new IOException(e);
      }

      ForkProperties forkProps = ForkProperties.parse(uri);
      setFork(ForkInitializer.init(networkID, forkProps, genesisTimestamp));
    }
    return fork;
  }

  public void setFork(Fork fork) {
    this.fork = fork;
  }

  public JrpcServiceProxyFactory getProxyFactory()
      throws SQLException, IOException, ClassNotFoundException {
    if (proxyFactory == null) {
      Map<String, String> clazzMap = new HashMap<>();
      Map<String, String> clazzMapImpl = new HashMap<>();

      clazzMap.put("org.eontechnology.and.peer.core.api.IMetadataService", "metadata");
      clazzMapImpl.put(
          "org.eontechnology.and.peer.core.api.IMetadataService",
          "org.eontechnology.and.eon.app.jsonrpc.proxy.MetadataServiceProxy");

      clazzMap.put(
          "org.eontechnology.and.peer.core.api.ITransactionSynchronizationService", "transactions");
      clazzMapImpl.put(
          "org.eontechnology.and.peer.core.api.ITransactionSynchronizationService",
          "org.eontechnology.and.eon.app.jsonrpc.proxy.TransactionSynchronizationServiceProxy");

      clazzMap.put("org.eontechnology.and.peer.core.api.IBlockSynchronizationService", "blocks");
      clazzMapImpl.put(
          "org.eontechnology.and.peer.core.api.IBlockSynchronizationService",
          "org.eontechnology.and.eon.app.jsonrpc.proxy.BlockSynchronizationServiceProxy");

      clazzMap.put(
          "org.eontechnology.and.peer.core.api.ISnapshotSynchronizationService", "snapshot");
      clazzMapImpl.put(
          "org.eontechnology.and.peer.core.api.ISnapshotSynchronizationService",
          "org.eontechnology.and.eon.app.jsonrpc.proxy.SnapshotSynchronizationServiceProxy");

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

  public BlockGenerator getBlockGenerator()
      throws SQLException, IOException, ClassNotFoundException {
    if (blockGenerator == null) {
      setBlockGenerator(
          new BlockGenerator(
              getFork(),
              getSigner(),
              getBacklog(),
              getBlockchainProvider(),
              getLedgerProvider(),
              getTransactionValidatorFabric(),
              getEstimator(),
              getAccountHelper()));
    }
    return blockGenerator;
  }

  public void setBlockGenerator(BlockGenerator blockGenerator)
      throws SQLException, IOException, ClassNotFoundException {
    this.blockGenerator = blockGenerator;
  }

  public BacklogCleaner getCleaner() throws SQLException, IOException, ClassNotFoundException {
    if (cleaner == null) {
      setCleaner(new BacklogCleaner(getBacklog()));
    }
    return cleaner;
  }

  public void setCleaner(BacklogCleaner cleaner)
      throws SQLException, IOException, ClassNotFoundException {
    this.cleaner = cleaner;
  }

  public CryptoProvider getSignatureVerifier()
      throws SQLException, IOException, ClassNotFoundException {
    if (cryptoProvider == null) {
      setSignatureVerifier(CryptoProvider.getInstance());
    }
    return cryptoProvider;
  }

  public void setSignatureVerifier(CryptoProvider cryptoProvider) {
    this.cryptoProvider = cryptoProvider;
  }

  public LedgerProvider getLedgerProvider()
      throws SQLException, IOException, ClassNotFoundException {
    if (ledgerProvider == null) {
      setLedgerProvider(new LedgerProvider(getStorage(), getFork()));
    }
    return ledgerProvider;
  }

  public void setLedgerProvider(LedgerProvider ledgerProvider) {
    this.ledgerProvider = ledgerProvider;
  }

  public BlockchainEventManager getBlockchainEventManager()
      throws SQLException, IOException, ClassNotFoundException {
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

  public TransactionValidatorFabricImpl getTransactionValidatorFabric()
      throws SQLException, IOException, ClassNotFoundException {
    if (transactionValidatorFabric == null) {
      setTransactionValidatorFabric(
          new TransactionValidatorFabricImpl(getFork(), getAccountHelper()));
    }
    return transactionValidatorFabric;
  }

  public void setTransactionValidatorFabric(
      TransactionValidatorFabricImpl transactionValidatorFabric) {
    this.transactionValidatorFabric = transactionValidatorFabric;
  }

  public ITransactionEstimator getEstimator() {
    if (estimator == null) {
      setEstimator(new TransactionEstimator(CryptoProvider.getInstance().getFormatter()));
    }
    return estimator;
  }

  public void setEstimator(ITransactionEstimator estimator) {
    this.estimator = estimator;
  }

  public IAccountHelper getAccountHelper()
      throws SQLException, IOException, ClassNotFoundException {
    if (accountHelper == null) {
      setAccountHelper(new AccountHelper(getFork()));
    }
    return accountHelper;
  }

  public void setAccountHelper(IAccountHelper accountHelper) {
    this.accountHelper = accountHelper;
  }

  public ITransactionMapper getTransactionMapper()
      throws SQLException, IOException, ClassNotFoundException {
    if (transactionMapper == null) {
      transactionMapper = new TransactionMapper(getStorage(), getFork());
    }
    return transactionMapper;
  }

  public void setTransactionMapper(ITransactionMapper transactionMapper) {
    this.transactionMapper = transactionMapper;
  }
}
