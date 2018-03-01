package com.exscudo.peer.core.storage.utils;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import com.j256.ormlite.support.ConnectionSource;

/**
 * Manage block in DB
 */
public class BlockHelper {
    private Dao<DbBlock, Long> daoBlocks;
    private Dao<DbTransaction, Long> daoTransaction;

    private QueryBuilder<DbBlock, Long> getPrevIdBuilder = null;
    private QueryBuilder<DbTransaction, Long> containsTransactionBuilder = null;
    private QueryBuilder<DbBlock, Long> getForkedBlocksBuilder = null;
    private QueryBuilder<DbBlock, Long> getBuilder = null;
    private DeleteBuilder<DbBlock, Long> deleteBlockBuilder = null;
    private QueryBuilder<DbTransaction, Long> getTxByBlockBuilder = null;
    private DeleteBuilder<DbTransaction, Long> delTxByBlockBuilder = null;

    private ArgumentHolder vID = new ThreadLocalSelectArg();
    private ArgumentHolder vTxID = new ThreadLocalSelectArg();
    private ArgumentHolder vTxBlockID = new ThreadLocalSelectArg();
    private ArgumentHolder vTimestamp = new ThreadLocalSelectArg();

    public BlockHelper(ConnectionSource connectionSource) {
        try {
            daoBlocks = DaoManager.createDao(connectionSource, DbBlock.class);
            daoTransaction = DaoManager.createDao(connectionSource, DbTransaction.class);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Block get(BlockID blockID) throws SQLException {

        DbBlock dbBlock = getBlock(blockID.getValue());
        if (dbBlock == null) {
            return null;
        }

        Block block = dbBlock.toBlock(this);
        return block;
    }

    public BlockID getPreviousBlockId(BlockID blockID) throws SQLException {

        if (getPrevIdBuilder == null) {
            getPrevIdBuilder = daoBlocks.queryBuilder();
            getPrevIdBuilder.selectColumns("previous_block_id");
            getPrevIdBuilder.where().eq("id", vID);
        }

        vID.setValue(blockID.getValue());
        DbBlock dbBlock = getPrevIdBuilder.queryForFirst();
        if (dbBlock == null) {
            return null;
        }
        return new BlockID(dbBlock.getPreviousBlock());
    }

    public boolean containsTransaction(BlockID blockID, TransactionID transactionID) throws SQLException {

        if (containsTransactionBuilder == null) {
            containsTransactionBuilder = daoTransaction.queryBuilder();
            containsTransactionBuilder.where().eq("id", vTxID).and().eq("block_id", vTxBlockID);
        }

        vTxID.setValue(transactionID.getValue());
        vTxBlockID.setValue(blockID.getValue());

        long count = containsTransactionBuilder.countOf();
        return count != 0;
    }

    public List<BlockID> getForkedBlocksIds(int timestamp) throws SQLException {

        if (getForkedBlocksBuilder == null) {
            getForkedBlocksBuilder = daoBlocks.queryBuilder();
            getForkedBlocksBuilder.selectColumns("id");
            getForkedBlocksBuilder.where().lt("timestamp", vTimestamp).and().eq("tag", 0);
        }

        vTimestamp.setValue(timestamp);
        List<DbBlock> dbBlocks = getForkedBlocksBuilder.query();
        LinkedList<BlockID> res = new LinkedList<>();
        for (DbBlock block : dbBlocks) {
            res.add(new BlockID(block.getId()));
        }

        return res;
    }

    public void remove(BlockID blockID) throws SQLException {

        removeBlock(blockID.getValue());
        removeTransactions(blockID.getValue());
    }

    public void save(Block newBlock) throws SQLException {

        DbBlock dbBlock = new DbBlock(newBlock);
        dbBlock.setTag(0);

        long newBlockID = newBlock.getID().getValue();
        int newBlockHeight = newBlock.getHeight();
        for (Transaction tx : newBlock.getTransactions()) {

            DbTransaction dbTx = new DbTransaction(tx);
            dbTx.setHeight(newBlockHeight);
            dbTx.setBlockID(newBlockID);
            dbTx.setTag(0);
            saveTransaction(dbTx);
        }
        saveBlock(dbBlock);
    }

    /**
     * Read block from DB
     *
     * @param id  block id to read
     * @return block from DB
     * @throws SQLException problems with the DB
     */
    DbBlock getBlock(long id) throws SQLException {

        if (getBuilder == null) {
            getBuilder = daoBlocks.queryBuilder();
            getBuilder.where().eq("id", vID);
        }

        vID.setValue(id);
        return getBuilder.queryForFirst();
    }

    /**
     * Remove block from DB
     *
     * @param id  block id to remove
     * @throws SQLException problems with the DB
     */
    void removeBlock(long id) throws SQLException {

        if (deleteBlockBuilder == null) {
            deleteBlockBuilder = daoBlocks.deleteBuilder();
            deleteBlockBuilder.where().eq("id", vID);
        }

        vID.setValue(id);
        deleteBlockBuilder.delete();
    }

    /**
     * Save block to DB
     *
     * @param dbBlock Block to save
     * @throws SQLException problems with the DB
     */
    void saveBlock(DbBlock dbBlock) throws SQLException {
        daoBlocks.create(dbBlock);
    }

    List<DbTransaction> getTransactions(BlockID blockID) throws SQLException {

        if (getTxByBlockBuilder == null) {
            getTxByBlockBuilder = daoTransaction.queryBuilder();
            getTxByBlockBuilder.where().eq("block_id", vTxBlockID);
        }

        vTxBlockID.setValue(blockID.getValue());
        return getTxByBlockBuilder.query();
    }

    void removeTransactions(long blockID) throws SQLException {

        if (delTxByBlockBuilder == null) {
            delTxByBlockBuilder = daoTransaction.deleteBuilder();
            delTxByBlockBuilder.where().eq("block_id", vTxBlockID);
        }

        vTxBlockID.setValue(blockID);
        delTxByBlockBuilder.delete();
    }

    void saveTransaction(DbTransaction transaction) throws SQLException {

        daoTransaction.create(transaction);
    }
}
