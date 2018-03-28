package com.exscudo.peer.core.importer.tasks;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.api.ISnapshotSynchronizationService;
import com.exscudo.peer.core.blockchain.BlockchainProvider;
import com.exscudo.peer.core.blockchain.storage.DbBlock;
import com.exscudo.peer.core.blockchain.storage.DbTransaction;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.env.Peer;
import com.exscudo.peer.core.ledger.Ledger;
import com.exscudo.peer.core.ledger.storage.DbNode;
import com.exscudo.peer.core.storage.Storage;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;

public class SyncSnapshotTask implements Runnable {

    private final Storage storage;
    private final ExecutionContext context;
    private final IFork fork;
    private final BlockchainProvider blockchainProvider;
    private boolean disabled = false;
    private Block genesis = null;

    public SyncSnapshotTask(Storage storage,
                            ExecutionContext context,
                            IFork fork,
                            BlockchainProvider blockchainProvider) {

        this.storage = storage;
        this.context = context;
        this.fork = fork;
        this.blockchainProvider = blockchainProvider;
    }

    @Override
    public void run() {

        if (disabled) {
            Loggers.info(SyncSnapshotTask.class, "Snapshot exist, disabling task...");
            throw new RuntimeException("Disabling SyncSnapshotTask");
        }

        Peer peer = null;
        try {

            if (storage.metadata().getHistoryFromHeight() >= 0) {
                disabled = true;
                return;
            }

            peer = context.getAnyConnectedPeer();
            if (peer == null) {
                return;
            }

            ISnapshotSynchronizationService service = peer.getSnapshotSynchronizationService();
            Block lastBlock = service.getLastBlock();
            fillHeight(lastBlock);

            int targetHeight = lastBlock.getHeight() - Constant.BLOCK_IN_DAY;
            if (targetHeight <= 0) {
                Loggers.info(SyncSnapshotTask.class, "Short history, load full blockchain");
                storage.metadata().setHistoryFromHeight(0);
                disabled = true;
                return;
            }

            final Block block = service.getBlockByHeight(targetHeight);
            fillHeight(block);

            if (block == null) {
                throw new RemotePeerException("Empty history on remote peer");
            }

            Loggers.info(SyncSnapshotTask.class, "Loaded start block: [{}]{}", block.getHeight(), block.getID());

            storage.callInTransaction(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    Dao<DbNode, ?> dao = DaoManager.createDao(storage.getConnectionSource(), DbNode.class);
                    DeleteBuilder<DbNode, ?> builder = dao.deleteBuilder();
                    builder.where().gt("timestamp", block.getTimestamp());
                    builder.delete();

                    return null;
                }
            });

            Ledger ledger =
                    new Ledger(storage.getConnectionSource(), null, block.getTimestamp() + Constant.BLOCK_PERIOD);

            Map<String, Object> accSetMap = service.getAccounts(block.getID().toString());

            Set<String> totalAccSet = new HashSet<>();

            Loggers.info(SyncSnapshotTask.class, "Loading accounts...");

            while (accSetMap != null) {
                boolean imported = false;
                BigInteger maxAcc = BigInteger.ZERO;

                for (String id : accSetMap.keySet()) {

                    if (totalAccSet.contains(id)) {
                        continue;
                    }

                    Map<String, Object> accMap = (Map<String, Object>) accSetMap.get(id);
                    Account acc = new Account(new AccountID(id));
                    for (String p : accMap.keySet()) {
                        Map<String, Object> data = (Map<String, Object>) accMap.get(p);
                        AccountProperty property = new AccountProperty(p, data);
                        acc = acc.putProperty(property);
                    }

                    ledger = ledger.putAccount(acc);

                    BigInteger reversed = new BigInteger(Long.toHexString(Long.reverse(acc.getID().getValue())), 16);
                    maxAcc = maxAcc.max(reversed);

                    totalAccSet.add(id);
                    imported = true;
                }

                if (imported) {
                    Ledger finalLedger = ledger;
                    storage.callInTransaction(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            try {
                                finalLedger.save();
                            } catch (Throwable tx) {
                                Loggers.error(SyncSnapshotTask.class, tx);
                            }
                            return null;
                        }
                    });

                    ledger = new Ledger(storage.getConnectionSource(),
                                        ledger.getHash(),
                                        block.getTimestamp() + Constant.BLOCK_PERIOD);

                    accSetMap = service.getNextAccounts(block.getID().toString(),
                                                        new AccountID(Long.reverse(maxAcc.longValue())).toString());

                    Loggers.info(SyncSnapshotTask.class, "Loaded accounts: {}", totalAccSet.size());
                } else {
                    accSetMap = null;
                }
            }

            Loggers.info(SyncSnapshotTask.class, "All accounts loaded: {}", totalAccSet.size());

            if (!block.getSnapshot().equals(ledger.getHash())) {
                throw new RemotePeerException("Incorrect snapshot");
            }

            Loggers.info(SyncSnapshotTask.class, "Loading block heads for day...");
            // Load history for day before
            HashMap<BlockID, Block> blockSet = new HashMap<>();

            int h = targetHeight - Constant.DIFFICULTY_DELAY;
            h = Math.max(h, 0);

            while (h < targetHeight - 1) {

                Block[] head = service.getBlocksHeadFrom(h + 1);

                for (Block b : head) {
                    fillHeight(b);

                    if (b.getHeight() < targetHeight) {
                        blockSet.put(b.getID(), b);
                    }

                    h = Math.max(h, b.getHeight());
                }
            }

            // Check EDS
            blockSet.put(block.getID(), block);

            Loggers.info(SyncSnapshotTask.class, "Validating signatures...");
            for (Block b : blockSet.values()) {
                Account generator = ledger.getAccount(b.getSenderID());
                byte[] publicKey = fork.getPublicKey(generator, b.getTimestamp());
                if (!b.verifySignature(publicKey)) {
                    throw new RemotePeerException("Bad block signature");
                }

                if (b.getTransactions() != null) {
                    for (Transaction tx : block.getTransactions()) {
                        Account account = ledger.getAccount(tx.getSenderID());
                        byte[] pk = fork.getPublicKey(account, b.getTimestamp());
                        if (!tx.verifySignature(pk)) {
                            throw new RemotePeerException("Bad transaction signature");
                        }
                    }
                }
            }

            Loggers.info(SyncSnapshotTask.class, "Saving...");
            // Save in DB
            Dao<DbBlock, Long> daoBlocks = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);
            Dao<DbTransaction, Long> daoTransactions =
                    DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);

            Ledger finalLedger = ledger;

            storage.callInTransaction(new Callable<Void>() {
                @Override
                public Void call() throws Exception {

                    for (Block b : blockSet.values()) {
                        daoBlocks.create(new DbBlock(b));
                        if (b.getTransactions() != null) {
                            for (Transaction transaction : b.getTransactions()) {
                                daoTransactions.create(new DbTransaction(transaction));
                            }
                        }
                    }

                    finalLedger.save();

                    daoBlocks.updateBuilder().updateColumnValue("tag", 1).update();
                    daoTransactions.updateBuilder().updateColumnValue("tag", 1).update();

                    storage.metadata().setLastBlockID(block.getID());
                    storage.metadata().setHistoryFromHeight(block.getHeight());
                    blockchainProvider.initialize();

                    return null;
                }
            });

            Loggers.info(SyncSnapshotTask.class, "Snapshot downloaded!");
            disabled = true;
        } catch (RemotePeerException th) {
            Loggers.error(SyncSnapshotTask.class, "Failed to sync with '" + peer + "'", th);
            Loggers.debug(SyncSnapshotTask.class, "The node is disconnected. \"{}\".", peer);

            context.blacklistPeer(peer);
            Loggers.error(SyncSnapshotTask.class, th);
        } catch (Throwable th) {
            Loggers.error(SyncSnapshotTask.class, "Failed to sync with '" + peer + "'", th);
            Loggers.error(SyncSnapshotTask.class, th);
        }
    }

    private void fillHeight(Block block) {
        if (genesis == null) {
            genesis = blockchainProvider.getBlock(blockchainProvider.getGenesisBlockID());
        }

        int height = (block.getTimestamp() - genesis.getTimestamp()) / Constant.BLOCK_PERIOD;
        block.setHeight(height);
    }
}
