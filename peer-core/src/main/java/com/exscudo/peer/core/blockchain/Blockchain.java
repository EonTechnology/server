package com.exscudo.peer.core.blockchain;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Callable;

import com.exscudo.peer.core.blockchain.storage.DbBlock;
import com.exscudo.peer.core.blockchain.storage.DbTransaction;
import com.exscudo.peer.core.blockchain.storage.converters.DTOConverter;
import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;
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

    private HashMap<BlockID, BlockID> prevCache = new HashMap<>();

    // region Prepared Statements

    private Dao<DbBlock, Long> daoBlocks;
    private Dao<DbTransaction, Long> daoTransactions;

    private QueryBuilder<DbBlock, Long> blockQueryBuilder = null;
    private QueryBuilder<DbBlock, Long> blockByHeightQueryBuilder = null;
    private QueryBuilder<DbBlock, Long> blockExistsQueryBuilder = null;
    private QueryBuilder<DbBlock, Long> prevBlockIdQueryBuilder = null;

    private ArgumentHolder vID = new ThreadLocalSelectArg();
    private ArgumentHolder vHeight = new ThreadLocalSelectArg();

    // endregion

    public Blockchain(Storage storage, Block block) {

        this.storage = storage;

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

        if (headBlock != null && !headBlock.getID().equals(newBlock.getPreviousBlock())) {
            throw new IllegalStateException("Unexpected block.");
        }

        return storage.callInTransaction(new Callable<Block>() {

            @Override
            public Block call() throws Exception {
                headBlock = doSave(newBlock);
                if (milestoneBlock == null) {
                    milestoneBlock = headBlock;
                }
                return headBlock;
            }
        });
    }

    public Block getByHeight(int height) {

        try {

            // Fast search in main blockchain
            if (height < milestoneBlock.getHeight()) {

                DbBlock targetBlock = getBlockByHeight(height);

                if (containsBlock(milestoneBlock.getID().getValue())) {
                    return DTOConverter.convert(targetBlock, storage);
                }
            }

            // Search in forked blockchain
            BlockID currBlockID = headBlock.getPreviousBlock();
            int currHeight = headBlock.getHeight() - 1;

            while (currHeight > height && currBlockID != null) {

                currBlockID = getPrevBlockID(currBlockID);
                currHeight--;
            }

            if (currBlockID != null && currHeight == height) {

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

                DbTransaction dbTx = DTOConverter.convert(tx);
                dbTx.setHeight(newBlock.getHeight());
                dbTx.setBlockID(newBlock.getID().getValue());
                dbTx.setTag(0);

                daoTransactions.create(dbTx);
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

    public BlockID getPrevBlockID(BlockID id) throws SQLException {

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

    public Block getMilestoneBlock() {
        return milestoneBlock;
    }
}
