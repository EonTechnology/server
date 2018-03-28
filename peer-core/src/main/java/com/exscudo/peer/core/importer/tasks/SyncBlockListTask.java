package com.exscudo.peer.core.importer.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.api.IBlockSynchronizationService;
import com.exscudo.peer.core.backlog.Backlog;
import com.exscudo.peer.core.blockchain.BlockchainProvider;
import com.exscudo.peer.core.blockchain.TransactionProvider;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.common.exceptions.LifecycleException;
import com.exscudo.peer.core.common.exceptions.ProtocolException;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.env.Peer;
import com.exscudo.peer.core.importer.IUnitOfWork;
import com.exscudo.peer.core.importer.UnitOfWork;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.core.storage.Storage;

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
    private final TimeProvider timeProvider;
    private final TransactionProvider transactionProvider;
    private final Backlog backlog;
    private LedgerProvider ledgerProvider;

    public SyncBlockListTask(IFork fork,
                             Backlog backlog,
                             Storage storage,
                             ExecutionContext context,
                             BlockchainProvider blockchainProvider,
                             TimeProvider timeProvider,
                             LedgerProvider ledgerProvider,
                             TransactionProvider transactionProvider) {
        this.storage = storage;
        this.context = context;
        this.blockchainProvider = blockchainProvider;
        this.fork = fork;
        this.backlog = backlog;
        this.timeProvider = timeProvider;
        this.ledgerProvider = ledgerProvider;
        this.transactionProvider = transactionProvider;
    }

    @Override
    public void run() {

        try {

            if (storage.metadata().getHistoryFromHeight() < 0) {
                return;
            }

            Peer peer = context.getAnyConnectedPeer();
            if (peer == null) {
                return;
            }

            try {

                IBlockSynchronizationService service = peer.getBlockSynchronizationService();
                Difficulty remoteState = service.getDifficulty();

                Difficulty currentState = new Difficulty(blockchainProvider.getLastBlock());

                if (remoteState.compareTo(currentState) == 0) {

                    // The chain of the blocks is synchronized with at
                    // least one node. Initiate an event and return.
                    Loggers.trace(SyncBlockListTask.class,
                                  "The chain of the blocks is synchronized with \"{}\".",
                                  peer);
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
        } catch (Exception e) {
            Loggers.error(SyncBlockListTask.class, e);
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
        IUnitOfWork uow = new UnitOfWork(blockchain, ledgerProvider, fork, commonBlock);
        try {

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
                                 "Block pushing... [{}] {} -> {}",
                                 newBlock.getHeight(),
                                 newBlock.getPreviousBlock(),
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
            } catch (Exception ignore) {
                Loggers.error(SyncBlockListTask.class, ignore);
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
        } catch (Throwable e) {

            throw e;
        }
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

        for (Transaction tx : block.getTransactions()) {
            Transaction fromBacklog = backlog.get(tx.getID());
            if (fromBacklog != null && Arrays.equals(fromBacklog.getSignature(), tx.getSignature())) {
                verified.add(fromBacklog);
                continue;
            }

            Transaction fromDB = transactionProvider.getTransaction(tx.getID());
            if (fromDB != null && Arrays.equals(fromDB.getSignature(), tx.getSignature())) {
                verified.add(fromDB);
                continue;
            }

            verified.add(tx);
        }

        block.setTransactions(verified);
    }
}