package com.exscudo.peer.core.blockchain;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.blockchain.storage.DbBlock;
import com.exscudo.peer.core.blockchain.storage.DbNestedTransaction;
import com.exscudo.peer.core.blockchain.storage.DbTransaction;
import com.exscudo.peer.core.blockchain.storage.converters.DTOConverter;
import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BaseIdentifier;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.middleware.TransactionParser;
import com.exscudo.peer.core.storage.Storage;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;

/**
 * Implementing a block chain that allows block adding
 */
public class Blockchain {
    private final Storage storage;

    private Block milestoneBlock;
    private Block headBlock;

    private HashMap<BlockID, Set<TransactionID>> tranCache = new HashMap<>();
    private HashMap<BlockID, BlockID> prevCache = new HashMap<>();
    private HashMap<BlockID, Set<TransactionID>> nestedTranCache = new HashMap<>();

    // region Prepared Statements

    private Dao<DbBlock, Long> daoBlocks;
    private Dao<DbTransaction, Long> daoTransactions;
    private Dao<DbNestedTransaction, Long> daoNestedTransactions;

    private QueryBuilder<DbBlock, Long> blockQueryBuilder = null;
    private QueryBuilder<DbBlock, Long> blockByHeightQueryBuilder = null;
    private QueryBuilder<DbBlock, Long> blockExistsQueryBuilder = null;
    private QueryBuilder<DbTransaction, Long> transactionsQueryBuilder = null;
    private QueryBuilder<DbBlock, Long> prevBlockIdQueryBuilder = null;
    private QueryBuilder<DbNestedTransaction, Long> nestedTransactionQueryBuilder = null;

    private ArgumentHolder vID = new ThreadLocalSelectArg();
    private ArgumentHolder vHeight = new ThreadLocalSelectArg();
    private ArgumentHolder vTxBlockID = new ThreadLocalSelectArg();
    private ArgumentHolder vNestedTxBlockID = new ThreadLocalSelectArg();

    // endregion

    public Blockchain(Storage storage, Block block) {

        this.storage = storage;

        this.milestoneBlock = block;
        this.headBlock = milestoneBlock;

        try {
            daoBlocks = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);
            daoTransactions = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);
            daoNestedTransactions = DaoManager.createDao(storage.getConnectionSource(), DbNestedTransaction.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized Block getLastBlock() {
        return headBlock;
    }

    public synchronized Block addBlock(Block newBlock) {

        if (headBlock != null && !headBlock.getID().equals(newBlock.getPreviousBlock())) {
            throw new IllegalStateException("Unexpected block.");
        }

        return storage.callInTransaction(new Callable<Block>() {

            @Override
            public Block call() throws Exception {
                headBlock = doSave(newBlock);
                return headBlock;
            }
        });
    }

    public Block getByHeight(int generationHeight) {

        try {

            // Fast search in main blockchain
            if (generationHeight < milestoneBlock.getHeight()) {

                DbBlock targetBlock = getBlockByHeight(generationHeight);

                if (containsBlock(milestoneBlock.getID().getValue())) {
                    return DTOConverter.convert(targetBlock, storage);
                }
            }

            // Search in forked blockchain
            BlockID currBlockID = headBlock.getPreviousBlock();
            int currHeight = headBlock.getHeight() - 1;

            while (currHeight > generationHeight && currBlockID != null) {

                currBlockID = getPrev(currBlockID);
                currHeight--;
            }

            if (currBlockID != null && currHeight == generationHeight) {

                DbBlock dbBlock = getBlock(currBlockID.getValue());
                if (dbBlock != null) {
                    return DTOConverter.convert(dbBlock, storage);
                }
            }

            return null;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public boolean containsTransaction(Transaction transaction) {

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

            // Simple search in headBlock
            for (Transaction tx : headBlock.getTransactions()) {
                if (tx.getID().equals(txID)) {
                    return true;
                }

                if (tx.hasNestedTransactions()) {

                    Set<String> aSet = new HashSet<>(tx.getNestedTransactions().keySet());
                    aSet.retainAll(set);
                    if (!aSet.isEmpty()) {
                        return true;
                    }
                }
            }

            // If not exist in headBlock - search in DB
            BlockID currBlockId = headBlock.getPreviousBlock();
            int timestamp = headBlock.getTimestamp() - Constant.BLOCK_PERIOD;

            while (timestamp > currTimestamp) {

                Set<TransactionID> idList = tranCache.get(currBlockId);

                if (idList == null) {
                    idList = getTransactionList(currBlockId);
                    tranCache.put(currBlockId, idList);
                }

                if (idList.contains(txID)) {
                    return true;
                }

                if (transaction.hasNestedTransactions()) {
                    Set<TransactionID> nestedIdList = nestedTranCache.get(currBlockId);

                    if (nestedIdList == null) {
                        nestedIdList = getNestedTransactionList(currBlockId);
                        nestedTranCache.put(currBlockId, nestedIdList);
                    }

                    for (String id : set) {
                        if (nestedIdList.contains(new TransactionID(id))) {
                            return true;
                        }
                    }
                }

                timestamp -= Constant.BLOCK_PERIOD;

                currBlockId = getPrev(currBlockId);
                Objects.requireNonNull(currBlockId);
            }

            return false;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    private Block doSave(Block newBlock) throws SQLException {

        DbBlock dbBlock = getBlock(newBlock.getID().getValue());
        if (dbBlock != null) {
            return DTOConverter.convert(dbBlock, storage);
        }

        DbBlock newDbBlock = DTOConverter.convert(newBlock);
        daoBlocks.create(newDbBlock);

        Collection<Transaction> list = newBlock.getTransactions();
        if (!list.isEmpty()) {

            for (Transaction tx : list) {

                AccountID recipientID;
                try {
                    recipientID = TransactionParser.getRecipient(tx);
                } catch (ValidateException e) {
                    throw new RuntimeException(e);
                }

                DbTransaction dbTx = DTOConverter.convert(tx);
                dbTx.setHeight(newBlock.getHeight());
                dbTx.setBlockID(newBlock.getID().getValue());
                dbTx.setTag(0);
                dbTx.setRecipientID(BaseIdentifier.getValueOrRef(recipientID));

                daoTransactions.create(dbTx);

                if (tx.hasNestedTransactions()) {

                    for (Transaction nestedTx : tx.getNestedTransactions().values()) {

                        DbNestedTransaction dbNestedTx = new DbNestedTransaction();
                        dbNestedTx.setBlockID(newBlock.getID().getValue());
                        dbNestedTx.setOwnerID(tx.getID().getValue());
                        dbNestedTx.setId(nestedTx.getID().getValue());

                        daoNestedTransactions.create(dbNestedTx);
                    }
                }
            }
        }

        return newBlock;
    }

    private DbBlock getBlock(long id) throws SQLException {

        if (blockQueryBuilder == null) {
            blockQueryBuilder = daoBlocks.queryBuilder();
            blockQueryBuilder.where().eq("id", vID);
        }

        vID.setValue(id);
        return blockQueryBuilder.queryForFirst();
    }

    private DbBlock getBlockByHeight(int height) throws SQLException {

        if (blockByHeightQueryBuilder == null) {
            blockByHeightQueryBuilder = daoBlocks.queryBuilder();
            blockByHeightQueryBuilder.where().eq("height", vHeight).and().eq("tag", 1);
        }

        vHeight.setValue(height);
        return blockByHeightQueryBuilder.queryForFirst();
    }

    private boolean containsBlock(long id) throws SQLException {

        if (blockExistsQueryBuilder == null) {
            blockExistsQueryBuilder = daoBlocks.queryBuilder();
            blockExistsQueryBuilder.where().eq("id", vID).and().eq("tag", 1);
            blockExistsQueryBuilder.selectColumns("height");
        }

        vID.setValue(id);
        long countOf = blockExistsQueryBuilder.countOf();
        return (countOf != 0);
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

    private Set<TransactionID> getNestedTransactionList(BlockID currBlockId) throws SQLException {

        if (nestedTransactionQueryBuilder == null) {
            nestedTransactionQueryBuilder = daoNestedTransactions.queryBuilder();
            nestedTransactionQueryBuilder.where().eq("block_id", vNestedTxBlockID);
            nestedTransactionQueryBuilder.selectColumns("id");
        }

        vNestedTxBlockID.setValue(currBlockId.getValue());

        HashSet<TransactionID> set = new HashSet<>();

        List<DbNestedTransaction> query = nestedTransactionQueryBuilder.query();
        for (DbNestedTransaction tx : query) {
            set.add(new TransactionID(tx.getId()));
        }

        return set;
    }

    private BlockID getPrev(BlockID id) throws SQLException {

        BlockID prevId = prevCache.get(id);
        if (prevId == null) {

            if (prevBlockIdQueryBuilder == null) {
                prevBlockIdQueryBuilder = daoBlocks.queryBuilder();
                prevBlockIdQueryBuilder.selectColumns("previous_block_id");
                prevBlockIdQueryBuilder.where().eq("id", vID);
            }

            vID.setValue(id.getValue());
            DbBlock dbBlock = prevBlockIdQueryBuilder.queryForFirst();
            if (dbBlock == null) {
                return null;
            }
            prevId = new BlockID(dbBlock.getPreviousBlock());

            prevCache.put(id, prevId);
        }

        return prevId;
    }
}
