package com.exscudo.peer.core.blockchain.tasks;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.blockchain.IBlockchainProvider;
import com.exscudo.peer.core.blockchain.storage.DbAccTransaction;
import com.exscudo.peer.core.blockchain.storage.DbBlock;
import com.exscudo.peer.core.blockchain.storage.DbTransaction;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.storage.Storage;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;

/**
 * Performs the task of removing side chains.
 */
public class BlockCleanerTask implements Runnable {

    private final Storage storage;
    private final IBlockchainProvider blockchainService;

    // region Prepared Statements

    private QueryBuilder<DbBlock, Long> blocksQueryBuilder = null;
    private DeleteBuilder<DbBlock, Long> blocksDeleteBuilder = null;
    private DeleteBuilder<DbTransaction, Long> transactionsDeleteBuilder = null;
    private DeleteBuilder<DbAccTransaction, Long> accTransactionsDeleteBuilder = null;

    private ArgumentHolder vTimestamp = new ThreadLocalSelectArg();
    private ArgumentHolder vID = new ThreadLocalSelectArg();
    private ArgumentHolder vBlockID = new ThreadLocalSelectArg();
    private ArgumentHolder vAccBlockID = new ThreadLocalSelectArg();
    private ArgumentHolder vTag = new ThreadLocalSelectArg();

    // endregion

    public BlockCleanerTask(IBlockchainProvider blockchainService, Storage storage) {

        this.blockchainService = blockchainService;
        this.storage = storage;
    }

    @Override
    public void run() {

        try {
            cleanTransactionAndBlock();
        } catch (Throwable e) {
            Loggers.error(BlockCleanerTask.class, "Unable to perform task.", e);
        }
    }

    private void cleanTransactionAndBlock() throws SQLException {

        List<BlockID> blocksIds = new LinkedList<>();

        blocksIds.addAll(getForkedBlocksIds());
        blocksIds.addAll(getOldHistoryBlocksIds());

        storage.callInTransaction(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                for (BlockID id : blocksIds) {
                    removeBlock(id.getValue());
                    removeAccTransactions(id.getValue());
                    removeTransactions(id.getValue());
                }
                return null;
            }
        });
    }

    private List<BlockID> getForkedBlocksIds() throws SQLException {

        initQueryBuilder();

        Block lastBlock = blockchainService.getLastBlock();
        int timestamp = lastBlock.getTimestamp() - Constant.SECONDS_IN_DAY;

        vTimestamp.setValue(timestamp);
        vTag.setValue(0);

        return getBlockIds(blocksQueryBuilder);
    }

    private List<BlockID> getBlockIds(QueryBuilder<DbBlock, Long> blocksQueryBuilder) throws SQLException {
        List<DbBlock> dbBlocks = blocksQueryBuilder.query();
        LinkedList<BlockID> res = new LinkedList<>();
        for (DbBlock block : dbBlocks) {
            res.add(new BlockID(block.getId()));
        }

        return res;
    }

    private List<BlockID> getOldHistoryBlocksIds() throws SQLException {

        if (storage.metadata().getProperty("FULL").equals("1")) {
            return new LinkedList<>();
        }

        Block lastBlock = blockchainService.getLastBlock();
        if (lastBlock.getHeight() <= Constant.STORAGE_FRAME_BLOCK) {
            return new LinkedList<>();
        }

        initQueryBuilder();

        storage.metadata().setHistoryFromHeight(lastBlock.getHeight() - Constant.STORAGE_FRAME_BLOCK);

        int timestamp = lastBlock.getTimestamp() - Constant.STORAGE_FRAME_BLOCK * Constant.BLOCK_PERIOD;

        vTimestamp.setValue(timestamp);
        vTag.setValue(1);

        return getBlockIds(blocksQueryBuilder);
    }

    private void initQueryBuilder() throws SQLException {
        if (blocksQueryBuilder == null) {

            Dao<DbBlock, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

            blocksQueryBuilder = dao.queryBuilder();
            blocksQueryBuilder.selectColumns("id");
            blocksQueryBuilder.where().lt("timestamp", vTimestamp).and().eq("tag", vTag).and().gt("height", 0);
        }
    }

    private void removeAccTransactions(long blockID) throws SQLException {

        if (accTransactionsDeleteBuilder == null) {

            Dao<DbAccTransaction, Long> dao =
                    DaoManager.createDao(storage.getConnectionSource(), DbAccTransaction.class);

            accTransactionsDeleteBuilder = dao.deleteBuilder();
            accTransactionsDeleteBuilder.where().eq("block_id", vAccBlockID);
        }

        vAccBlockID.setValue(blockID);
        accTransactionsDeleteBuilder.delete();
    }

    private void removeTransactions(long blockID) throws SQLException {

        if (transactionsDeleteBuilder == null) {

            Dao<DbTransaction, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);

            transactionsDeleteBuilder = dao.deleteBuilder();
            transactionsDeleteBuilder.where().eq("block_id", vBlockID);
        }

        vBlockID.setValue(blockID);
        transactionsDeleteBuilder.delete();
    }

    private void removeBlock(long id) throws SQLException {

        if (blocksDeleteBuilder == null) {
            Dao<DbBlock, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

            blocksDeleteBuilder = dao.deleteBuilder();
            blocksDeleteBuilder.where().eq("id", vID);
        }

        vID.setValue(id);
        blocksDeleteBuilder.delete();
    }
}
