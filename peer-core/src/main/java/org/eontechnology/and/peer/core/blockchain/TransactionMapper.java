package org.eontechnology.and.peer.core.blockchain;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.function.Function;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.IFork;
import org.eontechnology.and.peer.core.blockchain.storage.DbAccTransaction;
import org.eontechnology.and.peer.core.blockchain.storage.DbNestedTransaction;
import org.eontechnology.and.peer.core.blockchain.storage.DbTransaction;
import org.eontechnology.and.peer.core.common.exceptions.DataAccessException;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.data.identifier.TransactionID;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.core.storage.Storage;

public class TransactionMapper implements ITransactionMapper {
    private final WeakHashMap<Blockchain, Map<BlockID, Set<TransactionID>>> weakReferences = new WeakHashMap<>();

    private final IFork fork;
    private final Storage storage;

    // region Prepared Statements

    private Dao<DbNestedTransaction, Long> daoNestedTransactions;
    private Dao<DbAccTransaction, Long> daoAccTransactions;
    private Dao<DbTransaction, Long> daoTransactions;

    private QueryBuilder<DbTransaction, Long> transactionsQueryBuilder = null;
    private QueryBuilder<DbNestedTransaction, Long> nestedTransactionQueryBuilder = null;
    private QueryBuilder<DbAccTransaction, Long> blockQueryBuilder = null;
    private ArgumentHolder vTxBlockID = new ThreadLocalSelectArg();
    private ArgumentHolder vNestedTxBlockID = new ThreadLocalSelectArg();
    private ArgumentHolder vBlockID = new ThreadLocalSelectArg();

    // endregion

    public TransactionMapper(Storage storage, IFork fork) {
        this.storage = storage;
        this.fork = fork;

        try {
            daoTransactions = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);
            daoNestedTransactions = DaoManager.createDao(storage.getConnectionSource(), DbNestedTransaction.class);
            daoAccTransactions = DaoManager.createDao(storage.getConnectionSource(), DbAccTransaction.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BlockID getBlockID(Transaction transaction, Blockchain blockchain) {

        try {

            int currTimestamp = transaction.getTimestamp();
            TransactionID txID = transaction.getID();

            Set<String> set = new HashSet<>();
            if (transaction.hasNestedTransactions()) {

                for (Map.Entry<String, Transaction> e : transaction.getNestedTransactions().entrySet()) {
                    set.add(e.getKey());

                    Transaction tx = e.getValue();
                    currTimestamp = Math.min(currTimestamp, tx.getTimestamp());
                }
            }

            Block headBlock = blockchain.getLastBlock();

            // Simple search in headBlock
            for (Transaction tx : headBlock.getTransactions()) {
                if (tx.getID().equals(txID)) {
                    return headBlock.getID();
                }

                if (tx.hasNestedTransactions()) {

                    Set<String> aSet = new HashSet<>(tx.getNestedTransactions().keySet());
                    aSet.retainAll(set);
                    if (!aSet.isEmpty()) {
                        return headBlock.getID();
                    }
                }
            }

            Map<BlockID, Set<TransactionID>> cache = weakReferences.computeIfAbsent(blockchain,
                                                                                    new Function<Blockchain, Map<BlockID, Set<TransactionID>>>() {
                                                                                        @Override
                                                                                        public Map<BlockID, Set<TransactionID>> apply(
                                                                                                Blockchain blockchain) {
                                                                                            return new HashMap<BlockID, Set<TransactionID>>();
                                                                                        }
                                                                                    });
            // If not exist in headBlock - search in DB
            BlockID currBlockId = headBlock.getPreviousBlock();
            int timestamp = headBlock.getTimestamp() - Constant.BLOCK_PERIOD;

            while (timestamp > currTimestamp) {

                Set<TransactionID> idList = cache.get(currBlockId);

                if (idList == null) {
                    idList = getTransactions(currBlockId);
                    cache.put(currBlockId, idList);
                }

                if (idList.contains(txID)) {
                    return currBlockId;
                }

                timestamp -= Constant.BLOCK_PERIOD;

                currBlockId = blockchain.getPrevBlockID(currBlockId);
                Objects.requireNonNull(currBlockId);
            }

            return null;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public synchronized void map(Block block) {

        if (block.getTransactions().isEmpty()) {
            return;
        }

        storage.callInTransaction(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                if (blockQueryBuilder == null) {
                    blockQueryBuilder = daoAccTransactions.queryBuilder();
                    blockQueryBuilder.where().eq("block_id", vBlockID);
                }

                vBlockID.setValue(block.getID().getValue());
                if (blockQueryBuilder.countOf() != 0) {
                    return null;
                }

                ITransactionParser parser = fork.getParser(block.getTimestamp());

                for (Transaction tx : block.getTransactions()) {
                    if (tx.hasNestedTransactions()) {

                        for (Transaction nestedTx : tx.getNestedTransactions().values()) {

                            DbNestedTransaction dbNestedTx = new DbNestedTransaction();
                            dbNestedTx.setBlockID(block.getID().getValue());
                            dbNestedTx.setHeight(block.getHeight());
                            dbNestedTx.setOwnerID(tx.getID().getValue());
                            dbNestedTx.setId(nestedTx.getID().getValue());

                            try {
                                daoNestedTransactions.create(dbNestedTx);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    HashSet<AccountID> rep = new HashSet<>();
                    rep.add(tx.getSenderID());
                    rep.add(tx.getPayer());

                    try {
                        Collection<AccountID> recipients = parser.getDependencies(tx);
                        if (recipients != null) {
                            rep.addAll(recipients);
                        }
                    } catch (ValidateException e) {
                        throw new RuntimeException(e);
                    }

                    for (AccountID id : rep) {
                        if (id != null) {
                            DbAccTransaction dbAccTransaction = new DbAccTransaction();
                            dbAccTransaction.setAccountID(id.getValue());
                            dbAccTransaction.setTransactionID(tx.getID().getValue());
                            dbAccTransaction.setBlockID(block.getID().getValue());
                            dbAccTransaction.setTimestamp(tx.getTimestamp());
                            dbAccTransaction.setTag(0);

                            try {
                                daoAccTransactions.create(dbAccTransaction);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }

                return null;
            }
        });
    }

    private Set<TransactionID> getTransactions(BlockID blockID) {

        try {
            Set<TransactionID> set = getTransactionList(blockID);
            set.addAll(getNestedTransactionList(blockID));
            return set;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Set<TransactionID> getTransactionList(BlockID blockID) throws SQLException {

        if (transactionsQueryBuilder == null) {
            transactionsQueryBuilder = daoTransactions.queryBuilder();
            transactionsQueryBuilder.where().eq("block_id", vTxBlockID);
            transactionsQueryBuilder.selectColumns("id");
        }

        vTxBlockID.setValue(blockID.getValue());

        HashSet<TransactionID> set = new HashSet<>();

        List<DbTransaction> query = transactionsQueryBuilder.query();
        for (DbTransaction tx : query) {
            set.add(new TransactionID(tx.getId()));
        }

        return set;
    }

    private Set<TransactionID> getNestedTransactionList(BlockID blockID) throws SQLException {

        if (nestedTransactionQueryBuilder == null) {
            nestedTransactionQueryBuilder = daoNestedTransactions.queryBuilder();
            nestedTransactionQueryBuilder.where().eq("block_id", vNestedTxBlockID);
            nestedTransactionQueryBuilder.selectColumns("id");
        }

        vNestedTxBlockID.setValue(blockID.getValue());

        HashSet<TransactionID> set = new HashSet<>();

        List<DbNestedTransaction> query = nestedTransactionQueryBuilder.query();
        for (DbNestedTransaction tx : query) {
            set.add(new TransactionID(tx.getId()));
        }

        return set;
    }
}
