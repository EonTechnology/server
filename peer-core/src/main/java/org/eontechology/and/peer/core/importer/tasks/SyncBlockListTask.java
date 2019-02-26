package org.eontechology.and.peer.core.importer.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.eontechology.and.peer.core.Constant;
import org.eontechology.and.peer.core.IFork;
import org.eontechology.and.peer.core.api.Difficulty;
import org.eontechology.and.peer.core.api.IBlockSynchronizationService;
import org.eontechology.and.peer.core.backlog.Backlog;
import org.eontechology.and.peer.core.blockchain.BlockchainProvider;
import org.eontechology.and.peer.core.blockchain.ITransactionMapper;
import org.eontechology.and.peer.core.blockchain.storage.converters.StorageTransactionMapper;
import org.eontechology.and.peer.core.common.IAccountHelper;
import org.eontechology.and.peer.core.common.ITimeProvider;
import org.eontechology.and.peer.core.common.ITransactionEstimator;
import org.eontechology.and.peer.core.common.Loggers;
import org.eontechology.and.peer.core.common.exceptions.LifecycleException;
import org.eontechology.and.peer.core.common.exceptions.ProtocolException;
import org.eontechology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Block;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.BlockID;
import org.eontechology.and.peer.core.data.identifier.TransactionID;
import org.eontechology.and.peer.core.env.ExecutionContext;
import org.eontechology.and.peer.core.env.Peer;
import org.eontechology.and.peer.core.importer.IUnitOfWork;
import org.eontechology.and.peer.core.importer.UnitOfWork;
import org.eontechology.and.peer.core.ledger.LedgerProvider;
import org.eontechology.and.peer.core.middleware.TransactionValidatorFabric;
import org.eontechology.and.peer.core.storage.Storage;

/**
 * Performs the task of synchronizing the chain of the current node and a random
 * node.
 * <p>
 * In the first step, a random node for interaction is selected. The random node
 * selection algorithm must be described in a certain {@code ExecutionContext}
 * implementation. The second step, it compares difficulty of the chain at the
 * services and the current nodes. The synchronization process starts if the
 * "difficulty" of the last block of the services node is more than the
 * "difficulty" of the last block.
 */
public final class SyncBlockListTask implements Runnable {

    private final Storage storage;
    private final ExecutionContext context;
    private final BlockchainProvider blockchainProvider;
    private final IFork fork;
    private final ITimeProvider timeProvider;
    private final Backlog backlog;
    private final TransactionValidatorFabric transactionValidatorFabric;
    private final ITransactionEstimator estimator;
    private final IAccountHelper accountHelper;
    private final ITransactionMapper transactionMapper;
    private LedgerProvider ledgerProvider;

    public SyncBlockListTask(IFork fork,
                             Backlog backlog,
                             Storage storage,
                             ExecutionContext context,
                             BlockchainProvider blockchainProvider,
                             ITimeProvider timeProvider,
                             LedgerProvider ledgerProvider,
                             TransactionValidatorFabric transactionValidatorFabric,
                             ITransactionEstimator estimator,
                             IAccountHelper accountHelper,
                             ITransactionMapper transactionMapper) {
        this.storage = storage;
        this.context = context;
        this.blockchainProvider = blockchainProvider;
        this.fork = fork;
        this.backlog = backlog;
        this.timeProvider = timeProvider;
        this.ledgerProvider = ledgerProvider;
        this.transactionValidatorFabric = transactionValidatorFabric;
        this.estimator = estimator;
        this.accountHelper = accountHelper;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public void run() {

        try {

            if (storage.metadata().getHistoryFromHeight() < 0) {
                return;
            }

            List<Peer> peers = context.getAnyConnectedPeers(6);
            if (peers == null) {
                return;
            }

            CompletableFuture<?>[] futures = new CompletableFuture<?>[peers.size()];
            for (int i = 0; i < peers.size(); i++) {
                futures[i] = requestDifficulty(peers.get(i));
            }

            CompletableFuture.allOf(futures).join();

            Difficulty targetState = null;
            Peer targetPeer = null;
            for (int i = 0; i < futures.length; i++) {

                Peer currPeer = peers.get(i);
                Difficulty currState = (Difficulty) futures[i].get();
                if (currState == null) {
                    continue;
                }

                if (targetState == null || currState.compareTo(targetState) > 0) {
                    targetState = currState;
                    targetPeer = currPeer;
                }
            }

            if (targetPeer != null) {

                Loggers.info(SyncBlockListTask.class, "Target: " + targetPeer);
                sync(targetPeer);
            }
        } catch (Exception e) {
            Loggers.error(SyncBlockListTask.class, e);
        }
    }

    private CompletableFuture<?> requestDifficulty(final Peer peer) {

        return CompletableFuture.supplyAsync(new Supplier<Difficulty>() {

            @Override
            public Difficulty get() {

                try {
                    return peer.getBlockSynchronizationService().getDifficulty();
                } catch (IOException e) {
                    Loggers.warning(SyncBlockListTask.class, "Unable to get difficulty: " + peer, e);
                    return null;
                }
            }
        });
    }

    private void sync(Peer peer) throws Exception {

        Objects.requireNonNull(peer);

        try {

            IBlockSynchronizationService service = peer.getBlockSynchronizationService();
            Difficulty remoteState = service.getDifficulty();

            Difficulty currentState = new Difficulty(blockchainProvider.getLastBlock());

            if (remoteState.compareTo(currentState) == 0) {

                // The chain of the blocks is synchronized with at
                // least one node. Initiate an event and return.
                Loggers.trace(SyncBlockListTask.class, "The chain of the blocks is synchronized with \"{}\".", peer);
                context.raiseSynchronizedEvent(this, peer);
                return;
            }

            if (remoteState.compareTo(currentState) < 0) {

                // The "difficulty" of the chain at the services node is
                // lower than the current one. No need for synchronization.
                return;
            }

            // Starts the synchronization process, if the "difficulty" of
            // the last block of the randomly node more than the
            // "difficulty" of our last block.

            Loggers.info(SyncBlockListTask.class,
                         "Begining synchronization. Difficulty: [this] {}, [{}] {}. ",
                         currentState.getDifficulty(),
                         peer,
                         remoteState.getDifficulty());

            if (shortSyncScheme(service) != null) {
                return;
            }

            while (remoteState.compareTo(currentState) > 0) {

                Block headBlock = longSyncScheme(service);

                Difficulty currentStateNew = new Difficulty(headBlock);
                if (currentStateNew.compareTo(currentState) == 0) {
                    return;
                }
                currentState = currentStateNew;
                remoteState = service.getDifficulty();
            }
        } catch (RemotePeerException | IOException e) {

            context.disablePeer(peer);

            Loggers.trace(SyncBlockListTask.class, "Failed to execute a request. Target: " + peer, e);
            Loggers.debug(SyncBlockListTask.class, "The node is disconnected. \"{}\".", peer);
        } catch (ProtocolException e) {

            context.blacklistPeer(peer);

            Loggers.error(SyncBlockListTask.class, "Failed to sync with '" + peer + "'", e);
            Loggers.debug(SyncBlockListTask.class, "The node is disconnected. \"{}\".", peer);
            throw e;
        }
    }

    /**
     * Short synchronization scheme (synchronization of the last block only). In
     * most cases, if the network is in a "stable" state, the synchronization will
     * be performed exactly by the short scheme.
     * <p>
     * At first, the last block is requested on the services peer. In the second
     * step, searches for a block preceding the received from services node. If it
     * is contained in the chain of blocks on the current node, then its added to
     * the end of chain (if necessary, roll back the local chain of blocks to common
     * block). The criterion for adding the block is difficulty.
     *
     * @param service access point on a remote node that implements the block
     *                synchronization protocol.
     * @return true if the chain of blocks was synchronized, if a short
     * synchronization algorithm is not applicable - false
     * @throws IOException         Error during access to the services node.
     * @throws RemotePeerException Error during request processing on the services node. (e.g.
     *                             illegal arguments, invalid format, etc.)
     * @throws ProtocolException   The behavior of the services node does not match expectations.
     */
    private Block shortSyncScheme(IBlockSynchronizationService service) throws ProtocolException, IOException, RemotePeerException {

        Loggers.info(SyncBlockListTask.class, "ShortSyncScheme");

        Block newBlock = service.getLastBlock();
        Difficulty remoteState = new Difficulty(newBlock.getID(), newBlock.getCumulativeDifficulty());

        Difficulty currentState = new Difficulty(blockchainProvider.getLastBlock());

        if (remoteState.compareTo(currentState) <= 0) {
            throw new IllegalStateException("The state was changed.");
        }

        Block prevBlock = blockchainProvider.getBlock(newBlock.getPreviousBlock());
        if (prevBlock == null) {
            return null;
        }

        if (fork.isPassed(newBlock.getTimestamp())) {
            throw new ProtocolException("Incorrect FORK");
        }

        HashMap<BlockID, Block> futuresBlock = new HashMap<>();
        futuresBlock.put(newBlock.getPreviousBlock(), newBlock);
        return pushBlocks(blockchainProvider, prevBlock, futuresBlock);
    }

    /**
     * Long synchronization scheme (synchronization from the common block). This
     * synchronization mode is used usually when a node in unstable state (for
     * example, it is used during connected to a network).
     * <p>
     * In the process of synchronization, the chain is rolled back to the point of
     * division of the chain. Then the new ending imported. The maximum depth of
     * division is defined in {@link Constant#SYNC_MILESTONE_DEPTH}
     *
     * @param service access point on a remote node that implements the block
     *                synchronization protocol.
     * @throws IOException         Error during access to the services node.
     * @throws RemotePeerException Error during request processing on the services node. (e.g.
     *                             illegal arguments, invalid format, etc.)
     * @throws ProtocolException   The behavior of the services node does not match expectations.
     */
    private Block longSyncScheme(IBlockSynchronizationService service) throws ProtocolException, IOException, RemotePeerException {

        Loggers.info(SyncBlockListTask.class, "LongSyncScheme");

        Block lastBlock = blockchainProvider.getLastBlock();
        Difficulty beginState = new Difficulty(lastBlock);

        BlockID[] lastBlockIDs = blockchainProvider.getLatestBlocks(Constant.SYNC_SHORT_FRAME);
        Block[] items = service.getBlockHistory(blockIdEncode(lastBlockIDs));
        if (items.length == 0) {
            // Common blocks was not found... Requests blocks that have been
            // added since the last milestone.
            Loggers.warning(SyncBlockListTask.class, "Sync over a latest milestone.");
            lastBlockIDs = blockchainProvider.getLatestBlocks(Constant.SYNC_LONG_FRAME);
            items = service.getBlockHistory(blockIdEncode(lastBlockIDs));
        }

        Block commonBlock = getCommonBlockID(items);
        if (commonBlock == null) {
            throw new ProtocolException("Unable to get common block.");
        }

        List<Block> linked = getNextBlockchain(commonBlock, items);

        if (linked.size() == 0) {
            return lastBlock;
        }

        Map<BlockID, Block> futureBlocks = new HashMap<>();
        for (Block block : linked) {
            futureBlocks.put(block.getPreviousBlock(), block);
        }

        Block newBlock = linked.get(linked.size() - 1);
        Difficulty newState = new Difficulty(newBlock);

        int newSize = futureBlocks.size();
        while (newState.compareTo(beginState) < 0 && newSize == futureBlocks.size()) {

            lastBlockIDs = new BlockID[] {newBlock.getID()};
            items = service.getBlockHistory(blockIdEncode(lastBlockIDs));

            newSize = futureBlocks.size() + items.length;

            linked = getNextBlockchain(newBlock, items);

            if (linked.size() > 0) {

                for (Block block : linked) {
                    futureBlocks.put(block.getPreviousBlock(), block);
                }

                newBlock = linked.get(linked.size() - 1);
                newState = new Difficulty(newBlock);
            }
        }

        Loggers.warning(SyncBlockListTask.class,
                        "Target difficulty {} in {}",
                        newState.getDifficulty(),
                        newState.getLastBlockID());

        if (newState.compareTo(beginState) <= 0) {
            throw new ProtocolException(" Invalid difficulty. Before: " +
                                                beginState.getDifficulty() +
                                                ", after: " +
                                                newState.getDifficulty());
        }

        // There are blocks after the general block
        if (futureBlocks.isEmpty()) {
            return lastBlock;
        }

        return pushBlocks(blockchainProvider, commonBlock, futureBlocks);
    }

    private List<Block> getNextBlockchain(Block commonBlock, Block[] items) {

        Map<BlockID, Block> map = new HashMap<>();
        for (Block block : items) {
            map.put(block.getPreviousBlock(), block);
        }

        ArrayList<Block> next = new ArrayList<>(items.length);

        while (true) {
            Block newBlock = map.get(commonBlock.getID());

            if (newBlock == null) {
                return next;
            }

            if (fork.isPassed(newBlock.getTimestamp())) {
                return next;
            }

            if (newBlock.isFuture(timeProvider.get() + Constant.MAX_LATENCY)) {
                return next;
            }

            next.add(newBlock);
            newBlock.setHeight(commonBlock.getHeight() + 1);
            commonBlock = newBlock;
        }
    }

    private Block getCommonBlockID(Block[] items) {
        BlockID commonBlockID = null;
        int commonHeight = -1;

        for (Block block : items) {
            int height = blockchainProvider.getBlockHeight(block.getPreviousBlock());
            if (height > commonHeight) {
                commonHeight = height;
                commonBlockID = block.getPreviousBlock();
            }
        }

        if (commonBlockID == null) {
            return null;
        }

        return blockchainProvider.getBlock(commonBlockID);
    }

    private Block pushBlocks(BlockchainProvider blockchain,
                             Block commonBlock,
                             Map<BlockID, Block> futureBlocks) throws ProtocolException {

        if (blockchain.getBlock(commonBlock.getID()) == null) {
            throw new IllegalStateException();
        }

        Block lastBlock = blockchain.getLastBlock();
        if ((lastBlock.getHeight() - commonBlock.getHeight()) > Constant.SYNC_MILESTONE_DEPTH) {
            throw new IllegalStateException("Failed to remove blocks. Illegal depth.");
        }

        Block maxBlock = commonBlock;
        while (futureBlocks.containsKey(maxBlock.getID())) {
            maxBlock = futureBlocks.get(maxBlock.getID());
        }

        Difficulty currentState = new Difficulty(blockchain.getLastBlock());
        Difficulty targetState = new Difficulty(maxBlock);

        if (targetState.compareTo(currentState) <= 0) {
            return lastBlock;
        }

        Loggers.trace(SyncBlockListTask.class,
                      "Last block: [{}]{}. Common block: [{}]{}.",
                      lastBlock.getHeight(),
                      currentState.getLastBlockID(),
                      commonBlock.getHeight(),
                      commonBlock.getID());

        Block currBlock = commonBlock;
        IUnitOfWork uow = new UnitOfWork(blockchain,
                                         ledgerProvider,
                                         fork,
                                         commonBlock,
                                         transactionValidatorFabric,
                                         estimator,
                                         accountHelper,
                                         transactionMapper);

        try {

            BlockID newBlockID = currBlock.getID();
            while (futureBlocks.containsKey(newBlockID)) {

                Block newBlock = futureBlocks.get(newBlockID);
                if (newBlock.isFuture(timeProvider.get() + Constant.MAX_LATENCY)) {
                    throw new LifecycleException(newBlock.getID().toString());
                }
                if (fork.isPassed(newBlock.getTimestamp())) {
                    throw new LifecycleException("Incorrect FORK");
                }

                Loggers.info(SyncBlockListTask.class,
                             "Block pushing... [{}] {} (tx: {})-> {}",
                             newBlock.getHeight(),
                             newBlock.getPreviousBlock(),
                             newBlock.getTransactions() != null ? newBlock.getTransactions().size() : 0,
                             newBlock.getID());

                // Load verified transactions
                setVerifiedTransactions(newBlock);

                currBlock = uow.pushBlock(newBlock);
                newBlockID = currBlock.getID();

                Loggers.info(SyncBlockListTask.class,
                             "Block pushed: [{}] {} CD: {}",
                             currBlock.getHeight(),
                             newBlock.getID(),
                             currBlock.getCumulativeDifficulty());
            }
        } catch (ValidateException e) {
            throw new ProtocolException(e);
        } catch (Exception ex) {
            Loggers.error(SyncBlockListTask.class, ex);
        }

        Difficulty diff = new Difficulty(currBlock);

        // If node have imported all the blocks, the difficulty should match
        if (diff.getLastBlockID().equals(targetState.getLastBlockID()) &&
                !diff.getDifficulty().equals(targetState.getDifficulty())) {
            throw new ProtocolException("The Difficulty of the latest block is not valid.");
        }

        // If node have imported a part of the sequence
        if (diff.compareTo(currentState) > 0) {
            uow.commit();
            Loggers.info(SyncBlockListTask.class,
                         "Sync complete. [{}]{} -> [{}]{}",
                         commonBlock.getHeight(),
                         commonBlock.getID(),
                         currBlock.getHeight(),
                         diff.getLastBlockID());
        } else {

            // There were problems with the addition of the block. The node
            // is placed in the black list.
            throw new ProtocolException("Failed to sync. Before: " +
                                                currentState.getDifficulty() +
                                                ", after: " +
                                                diff.getDifficulty());
        }

        return currBlock;
    }

    /**
     * Encode id set to user-friendly strings
     *
     * @param ids
     * @return
     */
    private String[] blockIdEncode(BlockID[] ids) {

        String[] encoded = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            encoded[i] = ids[i].toString();
        }
        return encoded;
    }

    private void setVerifiedTransactions(Block block) {

        LinkedList<Transaction> verified = new LinkedList<>();

        Block lastBlock = blockchainProvider.getLastBlock();

        Map<TransactionID, Transaction> lastTrMap = new HashMap<>();
        if (lastBlock.getTransactions() != null) {
            for (Transaction tx : lastBlock.getTransactions()) {
                lastTrMap.put(tx.getID(), tx);
            }
        }

        for (Transaction tx : block.getTransactions()) {

            Map<String, Object> map = StorageTransactionMapper.convert(tx);

            Transaction fromBacklog = backlog.get(tx.getID());
            if (fromBacklog != null && map.equals(StorageTransactionMapper.convert(fromBacklog))) {
                verified.add(fromBacklog);
                continue;
            }

            Transaction fromLastTx = lastTrMap.get(tx.getID());
            if (fromLastTx != null && map.equals(StorageTransactionMapper.convert(fromLastTx))) {
                verified.add(fromLastTx);
                continue;
            }

            verified.add(tx);
        }

        block.setTransactions(verified);
    }
}