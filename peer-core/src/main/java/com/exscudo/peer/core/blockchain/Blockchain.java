package com.exscudo.peer.core.blockchain;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.blockchain.storage.DbBlock;
import com.exscudo.peer.core.blockchain.storage.DbTransaction;
import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BaseIdentifier;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.data.transaction.ITransactionHandler;
import com.exscudo.peer.core.storage.Storage;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;

public class Blockchain {
    private final Storage storage;
    private final IFork fork;

    private Block milestoneBlock;
    private Block headBlock;

    private HashMap<BlockID, Set<TransactionID>> tranCache = new HashMap<>();
    private HashMap<BlockID, BlockID> prevCache = new HashMap<>();

    // region Prepared Statements

    private Dao<DbBlock, Long> daoBlocks;
    private Dao<DbTransaction, Long> daoTransactions;

    private QueryBuilder<DbBlock, Long> blockQueryBuilder = null;
    private QueryBuilder<DbBlock, Long> blockByHeightQueryBuilder = null;
    private QueryBuilder<DbBlock, Long> blockExistsQueryBuilder = null;
    private QueryBuilder<DbTransaction, Long> transactionsQueryBuilder = null;
    private QueryBuilder<DbBlock, Long> prevBlockIdQueryBuilder = null;

    private ArgumentHolder vID = new ThreadLocalSelectArg();
    private ArgumentHolder vHeight = new ThreadLocalSelectArg();
    private ArgumentHolder vTxBlockID = new ThreadLocalSelectArg();

    // endregion

    public Blockchain(Storage storage, Block block, IFork fork) {

        this.storage = storage;
        this.fork = fork;

        this.milestoneBlock = block;
        this.headBlock = milestoneBlock;

        try {
            daoBlocks = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);
            daoTransactions = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized Block getLastBlock() {
        return headBlock;
    }

    public synchronized Block addBlock(Block newBlock) {

        if (!headBlock.getID().equals(newBlock.getPreviousBlock())) {
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
                    return targetBlock.toBlock(storage);
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
                    return dbBlock.toBlock(storage);
                }
            }

            return null;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public boolean containsTransaction(Transaction transaction) {

        try {
            TransactionID txID = transaction.getID();

            // Simple search in headBlock
            for (Transaction tx : headBlock.getTransactions()) {
                if (tx.getID().equals(txID)) {
                    return true;
                }
            }

            // If not exist in headBlock - search in DB
            BlockID currBlockId = headBlock.getPreviousBlock();
            int timestamp = headBlock.getTimestamp() - Constant.BLOCK_PERIOD;

            while (timestamp > transaction.getTimestamp()) {

                Set<TransactionID> idList = tranCache.get(currBlockId);

                if (idList == null) {
                    idList = getTransactionList(currBlockId);
                    tranCache.put(currBlockId, idList);
                }

                if (idList.contains(txID)) {
                    return true;
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

    private DbTransaction[] prepareTransactions(Block block) {

        Collection<Transaction> list = block.getTransactions();
        if (!list.isEmpty()) {

            ITransactionHandler handler = fork.getTransactionExecutor(block.getTimestamp());

            DbTransaction[] dbTransactions = new DbTransaction[list.size()];

            long blockID = block.getID().getValue();
            int heigth = block.getHeight();
            int index = 0;

            for (Transaction tx : list) {

                long recipientID = BaseIdentifier.getValueOrRef(handler.getRecipient(tx));

                DbTransaction dbTx = new DbTransaction(tx);
                dbTx.setHeight(heigth);
                dbTx.setBlockID(blockID);
                dbTx.setTag(0);
                dbTx.setRecipientID(recipientID);

                dbTransactions[index] = dbTx;
                index++;
            }

            return dbTransactions;
        }

        return new DbTransaction[0];
    }

    private Block doSave(Block newBlock) throws SQLException {

        DbBlock dbBlock = getBlock(newBlock.getID().getValue());
        if (dbBlock != null) {
            return dbBlock.toBlock(storage);
        }

        DbBlock newDbBlock = new DbBlock(newBlock);
        DbTransaction[] newDbTransactions = prepareTransactions(newBlock);

        daoBlocks.create(newDbBlock);
        for (DbTransaction tx : newDbTransactions) {
            daoTransactions.create(tx);
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
        DbBlock dbBlock = blockByHeightQueryBuilder.queryForFirst();
        return dbBlock;
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
