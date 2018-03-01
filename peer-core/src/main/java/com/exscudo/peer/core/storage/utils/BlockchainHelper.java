package com.exscudo.peer.core.storage.utils;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.support.ConnectionSource;

public class BlockchainHelper {

    private Dao<DbBlock, Long> daoBlocks;
    private Dao<DbTransaction, Long> daoTransaction;
    private BlockHelper blockHelper;

    private QueryBuilder<DbBlock, Long> getLinked = null;
    private QueryBuilder<DbBlock, Long> getByHBuilder = null;
    private QueryBuilder<DbBlock, Long> getHBuilder = null;
    private QueryBuilder<DbBlock, Long> existBuilder = null;
    private UpdateBuilder<DbTransaction, Long> txUpdateBuilder = null;
    private UpdateBuilder<DbBlock, Long> blockUpdateBuilder = null;

    private ArgumentHolder vTag = new ThreadLocalSelectArg();
    private ArgumentHolder vID = new ThreadLocalSelectArg();
    private ArgumentHolder vTxTag = new ThreadLocalSelectArg();
    private ArgumentHolder vTxID = new ThreadLocalSelectArg();
    private ArgumentHolder vHeightBegin = new ThreadLocalSelectArg();
    private ArgumentHolder vHeightEnd = new ThreadLocalSelectArg();
    private ArgumentHolder vHeight = new ThreadLocalSelectArg();

    public BlockchainHelper(ConnectionSource connectionSource) {
        blockHelper = new BlockHelper(connectionSource);
        try {
            daoBlocks = DaoManager.createDao(connectionSource, DbBlock.class);
            daoTransaction = DaoManager.createDao(connectionSource, DbTransaction.class);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Read block history from DB
     *
     * @param begin begin block height
     * @param end   end block height
     * @return sorted by height list of block id
     * @throws SQLException problems with the DB
     */
    public BlockID[] getBlockLinkedList(int begin, int end) throws SQLException {

        if (getLinked == null) {
            getLinked = daoBlocks.queryBuilder();
            getLinked.selectColumns("id");
            getLinked.where().eq("tag", 1).and().between("height", vHeightBegin, vHeightEnd);
            getLinked.orderBy("height", true);
        }

        vHeightBegin.setValue(begin);
        vHeightEnd.setValue(end);

        List<DbBlock> query = getLinked.query();
        List<BlockID> res = new LinkedList<>();

        for (DbBlock block : query) {
            res.add(new BlockID(block.getId()));
        }

        return res.toArray(new BlockID[0]);
    }

    public Block getByHeight(int height) throws SQLException {

        DbBlock dbBlock = getDbByHeight(height);
        if (dbBlock == null) {
            return null;
        }

        Block block = dbBlock.toBlock(blockHelper);
        return block;
    }

    private DbBlock getDbByHeight(int height) throws SQLException {

        if (getByHBuilder == null) {
            getByHBuilder = daoBlocks.queryBuilder();
            getByHBuilder.where().eq("height", vHeight).and().eq("tag", 1);
        }

        vHeight.setValue(height);
        DbBlock dbBlock = getByHBuilder.queryForFirst();
        return dbBlock;
    }

    public int getBlockHeight(BlockID id) throws SQLException {

        if (getHBuilder == null) {
            getHBuilder = daoBlocks.queryBuilder();
            getHBuilder.where().eq("id", vID).and().eq("tag", 1);
            getHBuilder.selectColumns("height");
        }

        vID.setValue(id.getValue());
        DbBlock block = getHBuilder.queryForFirst();

        if (block == null) {
            return -1;
        }
        return block.getHeight();
    }

    public void attachBlock(BlockID id) throws SQLException {
        setTag(id.getValue(), 1);
    }

    public void detachBlock(BlockID id) throws SQLException {
        setTag(id.getValue(), 0);
    }

    private void setTag(long blockID, int tag) throws SQLException {

        if (blockUpdateBuilder == null) {

            blockUpdateBuilder = daoBlocks.updateBuilder();
            blockUpdateBuilder.where().eq("id", vID);
            blockUpdateBuilder.updateColumnValue("tag", vTag);

            txUpdateBuilder = daoTransaction.updateBuilder();
            txUpdateBuilder.where().eq("block_id", vTxID);
            txUpdateBuilder.updateColumnValue("tag", vTxTag);
        }

        vID.setValue(blockID);
        vTag.setValue(tag);
        blockUpdateBuilder.update();

        vTxID.setValue(blockID);
        vTxTag.setValue(tag);
        txUpdateBuilder.update();
    }

    public boolean containBlock(BlockID blockID) throws SQLException {

        if (existBuilder == null) {
            existBuilder = daoBlocks.queryBuilder();
            existBuilder.where().eq("id", vID).and().eq("tag", 1);
        }

        vID.setValue(blockID.getValue());

        long countOf = existBuilder.countOf();
        return (countOf != 0);
    }
}
