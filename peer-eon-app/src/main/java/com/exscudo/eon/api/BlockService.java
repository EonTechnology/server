package com.exscudo.eon.api;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.exscudo.peer.core.blockchain.storage.DbBlock;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.storage.Storage;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;

public class BlockService {
    private final static int BLOCK_LAST_PAGE_SIZE = 10;

    private final Storage storage;

    private QueryBuilder<DbBlock, Long> getPageBuilder = null;
    private ArgumentHolder vPageBegin = new ThreadLocalSelectArg();
    private ArgumentHolder vPageEnd = new ThreadLocalSelectArg();

    private QueryBuilder<DbBlock, Long> getByHeightBuilder = null;
    private ArgumentHolder vHeight = new ThreadLocalSelectArg();

    private QueryBuilder<DbBlock, Long> getByIdBuilder = null;
    private ArgumentHolder vBlockID = new ThreadLocalSelectArg();

    private QueryBuilder<DbBlock, Long> getByAccountIdBuilder = null;
    private ArgumentHolder vAccountID = new ThreadLocalSelectArg();
    private ArgumentHolder vTimestamp = new ThreadLocalSelectArg();

    public BlockService(Storage storage) {
        this.storage = storage;
    }

    public List<Block> getLastPage() throws RemotePeerException, IOException {
        Block lastBlock = getLastBlock();
        return getPage(lastBlock.getHeight());
    }

    public List<Block> getPage(int height) throws RemotePeerException, IOException {

        int end = height;
        int begin = height - BLOCK_LAST_PAGE_SIZE + 1;

        if (begin < 0) {
            begin = 0;
        }
        if (end < 0) {
            end = 0;
        }

        ArrayList<Block> blocks = new ArrayList<>();
        try {

            if (getPageBuilder == null) {

                Dao dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

                getPageBuilder = dao.queryBuilder();
                getPageBuilder.where().eq("tag", 1).and().between("height", vPageBegin, vPageEnd);
                getPageBuilder.orderBy("height", true);
            }

            vPageBegin.setValue(begin);
            vPageEnd.setValue(end);

            List<DbBlock> query = getPageBuilder.query();
            for (DbBlock dbBlock : query) {

                Block block = dbBlock.toBlock(storage);
                block.setTransactions(new LinkedList<>());
                blocks.add(block);
            }
        } catch (Exception e) {

            Loggers.error(BlockService.class, e);
            throw new RemotePeerException();
        }

        blocks.sort(new BlockComparator());
        return blocks;
    }

    public Block getByHeight(int height) throws RemotePeerException, IOException {
        Block block = null;

        try {

            if (getByHeightBuilder == null) {

                Dao dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

                getByHeightBuilder = dao.queryBuilder();
                getByHeightBuilder.where().eq("height", vHeight).and().eq("tag", 1);
            }

            vHeight.setValue(height);

            DbBlock dbBlock = getByHeightBuilder.queryForFirst();
            if (dbBlock != null) {
                block = dbBlock.toBlock(storage);
                block.setTransactions(new LinkedList<>());
            }
        } catch (Exception e) {

            Loggers.error(BlockService.class, e);
            throw new RemotePeerException();
        }

        return block;
    }

    public Block getById(String id) throws RemotePeerException, IOException {

        Block block = null;

        try {

            BlockID blockID = new BlockID(id);

            if (getByIdBuilder == null) {

                Dao dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

                getByIdBuilder = dao.queryBuilder();
                getByIdBuilder.where().eq("id", vBlockID);
            }
            vBlockID.setValue(blockID.getValue());

            DbBlock dbBlock = getByIdBuilder.queryForFirst();
            if (dbBlock != null && dbBlock.getTag() != 0) {

                block = dbBlock.toBlock(storage);
                block.setTransactions(new LinkedList<>());
            }
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        } catch (Exception e) {

            Loggers.error(BlockService.class, e);
            throw new RemotePeerException();
        }

        return block;
    }

    public List<Block> getByAccountId(String id) throws RemotePeerException, IOException {
        Block lastBlock = getLastBlock();
        return getByAccountId(id, lastBlock.getTimestamp() - (24 * 60 * 60));
    }

    public List<Block> getByAccountId(String id, int timestamp) throws RemotePeerException, IOException {

        List<Block> items = new ArrayList<>();

        try {

            AccountID accountID = new AccountID(id);

            if (getByAccountIdBuilder == null) {

                Dao dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

                getByAccountIdBuilder = dao.queryBuilder();
                getByAccountIdBuilder.where().eq("tag", 1);
                getByAccountIdBuilder.where().and().eq("generator", vAccountID);
                getByAccountIdBuilder.where().and().ge("timestamp", vTimestamp);
                getByAccountIdBuilder.orderBy("height", false);
            }

            vAccountID.setValue(accountID.getValue());
            vTimestamp.setValue(timestamp);

            List<DbBlock> query = getByAccountIdBuilder.query();
            for (DbBlock dbBlock : query) {

                if (dbBlock != null) {

                    Block block = dbBlock.toBlock(storage);
                    block.setTransactions(new LinkedList<>());
                    items.add(block);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        } catch (Exception e) {

            Loggers.error(BlockService.class, e);
            throw new RemotePeerException();
        }

        return items;
    }

    /**
     * Read block history from DB generated by specified account
     *
     * @param peerAccountID generator id
     * @param time          time limit for read from history
     * @return sorted by height list of block id
     * @throws SQLException problems with the DB
     */
    public BlockID[] getCreatedBlockList(AccountID peerAccountID, int time) throws SQLException {

        if (getByAccountIdBuilder == null) {

            Dao dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

            getByAccountIdBuilder = dao.queryBuilder();
            getByAccountIdBuilder.selectColumns("id");
            getByAccountIdBuilder.where().eq("tag", 1);
            getByAccountIdBuilder.where().and().eq("generator", vAccountID);
            getByAccountIdBuilder.where().and().ge("timestamp", vTimestamp);
            getByAccountIdBuilder.orderBy("height", false);
        }

        vAccountID.setValue(peerAccountID.getValue());
        vTimestamp.setValue(time);

        List<DbBlock> query = getByAccountIdBuilder.query();
        List<BlockID> res = new LinkedList<>();

        for (DbBlock block : query) {
            res.add(new BlockID(block.getId()));
        }

        return res.toArray(new BlockID[0]);
    }

    public Block getLastBlock() throws RemotePeerException, IOException {

        try {
            BlockID blockID = storage.metadata().getLastBlockID();
            if (getByIdBuilder == null) {

                Dao dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

                getByIdBuilder = dao.queryBuilder();
                getByIdBuilder.where().eq("id", vBlockID);
            }
            vBlockID.setValue(blockID.getValue());
            DbBlock dbBlock = getByIdBuilder.queryForFirst();
            return dbBlock.toBlock(storage);
        } catch (Exception e) {

            Loggers.error(BlockService.class, e);
            throw new RemotePeerException();
        }
    }

    private static class BlockComparator implements Comparator<Block> {

        @Override
        public int compare(Block o1, Block o2) {
            return Integer.compare(o2.getHeight(), o1.getHeight());
        }
    }
}
