package org.eontechnology.and.peer.core.blockchain;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.api.Difficulty;
import org.eontechnology.and.peer.core.blockchain.events.BlockchainEventManager;
import org.eontechnology.and.peer.core.blockchain.storage.DbAccTransaction;
import org.eontechnology.and.peer.core.blockchain.storage.DbBlock;
import org.eontechnology.and.peer.core.blockchain.storage.DbTransaction;
import org.eontechnology.and.peer.core.blockchain.storage.converters.DTOConverter;
import org.eontechnology.and.peer.core.common.exceptions.DataAccessException;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.storage.Storage;

/**
 * Basic implementation of {@code IBlockchainProvider}.
 */
public class BlockchainProvider implements IBlockchainProvider {
    private final Storage storage;
    private final BlockchainEventManager blockchainEventManager;

    private volatile Block lastBlock;

    // region Prepared Statements

    private Dao<DbBlock, Long> daoBlocks;
    private Dao<DbTransaction, Long> daoTransactions;
    private Dao<DbAccTransaction, Long> daoAccTransactions;

    private QueryBuilder<DbBlock, Long> getBuilder = null;
    private UpdateBuilder<DbTransaction, Long> txUpdateBuilder = null;
    private UpdateBuilder<DbAccTransaction, Long> txAccUpdateBuilder = null;
    private UpdateBuilder<DbBlock, Long> blockUpdateBuilder = null;
    private QueryBuilder<DbBlock, Long> getByHBuilder = null;
    private QueryBuilder<DbBlock, Long> getHBuilder = null;
    private QueryBuilder<DbBlock, Long> getLinked = null;
    private QueryBuilder<DbBlock, Long> getPrevIdBuilder = null;

    private ArgumentHolder vID = new ThreadLocalSelectArg();
    private ArgumentHolder vTag = new ThreadLocalSelectArg();
    private ArgumentHolder vBlockID = new ThreadLocalSelectArg();
    private ArgumentHolder vTxTag = new ThreadLocalSelectArg();
    private ArgumentHolder vAccBlockID = new ThreadLocalSelectArg();
    private ArgumentHolder vAccTag = new ThreadLocalSelectArg();
    private ArgumentHolder vHeight = new ThreadLocalSelectArg();
    private ArgumentHolder vHeightBegin = new ThreadLocalSelectArg();
    private ArgumentHolder vHeightEnd = new ThreadLocalSelectArg();

    private QueryBuilder<DbTransaction, Long> forkedBuilder;
    private ArgumentHolder vTimestamp = new ThreadLocalSelectArg();
    private ArgumentHolder vTimestampSub = new ThreadLocalSelectArg();

    // endregion

    public BlockchainProvider(Storage storage, BlockchainEventManager blockchainEventManager) throws SQLException {
        this.storage = storage;
        this.blockchainEventManager = blockchainEventManager;

        try {

            daoBlocks = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);
            daoTransactions = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);
            daoAccTransactions = DaoManager.createDao(storage.getConnectionSource(), DbAccTransaction.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void initialize() throws SQLException {
        Storage.Metadata metadata = storage.metadata();
        lastBlock = getBlock(metadata.getLastBlockID());
    }

    // TODO: metadata as second parameter
    public void initialize(Blockchain blockchain, BlockID genesisBlockID, int historyHeight) {

        Block newHead = blockchain.getLastBlock();
        Block milestone = blockchain.getMilestoneBlock();
        storage.callInTransaction(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                attachTo(milestone, newHead);

                storage.metadata().setLastBlockID(newHead.getID());
                storage.metadata().setGenesisBlock(genesisBlockID);
                storage.metadata().setHistoryFromHeight(historyHeight);

                lastBlock = newHead;
                return null;
            }
        });
    }

    @Override
    public synchronized Block getLastBlock() {
        return lastBlock;
    }

    @Override
    public Block getBlock(BlockID blockID) {

        try {

            if (getBuilder == null) {
                getBuilder = daoBlocks.queryBuilder();
                getBuilder.where().eq("id", vID).and().eq("tag", 1);
            }
            vID.setValue(blockID.getValue());

            DbBlock dbBlock = getBuilder.queryForFirst();
            if (dbBlock != null) {
                return DTOConverter.convert(dbBlock, storage);
            }

            return null;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public Block getBlockByHeight(int height) {

        try {

            if (getByHBuilder == null) {
                getByHBuilder = daoBlocks.queryBuilder();
                getByHBuilder.where().eq("height", vHeight).and().eq("tag", 1);
            }
            vHeight.setValue(height);

            DbBlock dbBlock = getByHBuilder.queryForFirst();
            if (dbBlock != null) {
                return DTOConverter.convert(dbBlock, storage);
            }

            return null;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public int getBlockHeight(BlockID id) {
        try {

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
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public BlockID[] getLatestBlocks(int limit) {
        try {
            int height = lastBlock.getHeight();
            return getBlockLinkedList(height - limit + 1, height);
        } catch (SQLException e) {
            throw new DataAccessException(e);
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
    private BlockID[] getBlockLinkedList(int begin, int end) throws SQLException {

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

    public synchronized Block setLastBlock(Block newLastBlock) {

        Difficulty diffNew = new Difficulty(newLastBlock);
        Difficulty currNew = new Difficulty(getLastBlock());
        if (diffNew.compareTo(currNew) > 0) {
            setPointerTo(newLastBlock);
        }

        return lastBlock;
    }

    protected void setPointerTo(Block newHead) {

        final Block oldHead = getLastBlock();

        List<DbTransaction> forked = storage.callInTransaction(new Callable<List<DbTransaction>>() {
            @Override
            public List<DbTransaction> call() throws Exception {

                changeHead(oldHead, newHead);
                storage.metadata().setLastBlockID(newHead.getID());
                lastBlock = newHead;

                return getForkedTransactions(newHead.getTimestamp() - Constant.SECONDS_IN_DAY);
            }
        });

        if (!oldHead.getID().equals(getLastBlock().getID())) {

            LinkedList<Transaction> list = null;
            if (forked != null) {
                // Convert List<DbTransaction> to List<Transaction>
                list = new LinkedList<>();
                for (DbTransaction tx : forked) {
                    list.add(DTOConverter.convert(tx));
                }
            }
            blockchainEventManager.raiseBlockchainChanged(this, newHead, list);
        }
    }

    private void changeHead(Block oldHead, Block newHead) throws SQLException {

        BlockID milestoneBlockID = intersect(oldHead, newHead);
        BlockID currentBlockID = oldHead.getID();

        while (!currentBlockID.equals(milestoneBlockID)) {
            detachBlock(currentBlockID);
            currentBlockID = getPreviousBlockId(currentBlockID);
            if (currentBlockID == null) {
                throw new IllegalStateException();
            }
        }

        currentBlockID = newHead.getID();
        while (!currentBlockID.equals(milestoneBlockID)) {
            attachBlock(currentBlockID);
            currentBlockID = getPreviousBlockId(currentBlockID);
            if (currentBlockID == null) {
                throw new IllegalStateException();
            }
        }
    }

    private BlockID getPreviousBlockId(BlockID blockID) throws SQLException {

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

    private void attachBlock(BlockID id) throws SQLException {
        setTag(id.getValue(), 1);
    }

    private void detachBlock(BlockID id) throws SQLException {
        setTag(id.getValue(), 0);
    }

    private void setTag(long blockID, int tag) throws SQLException {

        if (blockUpdateBuilder == null) {
            blockUpdateBuilder = daoBlocks.updateBuilder();
            blockUpdateBuilder.where().eq("id", vID);
            blockUpdateBuilder.updateColumnValue("tag", vTag);

            txUpdateBuilder = daoTransactions.updateBuilder();
            txUpdateBuilder.where().eq("block_id", vBlockID);
            txUpdateBuilder.updateColumnValue("tag", vTxTag);

            txAccUpdateBuilder = daoAccTransactions.updateBuilder();
            txAccUpdateBuilder.where().eq("block_id", vAccBlockID);
            txAccUpdateBuilder.updateColumnValue("tag", vAccTag);
        }

        vTag.setValue(tag);
        vID.setValue(blockID);
        blockUpdateBuilder.update();

        vTxTag.setValue(tag);
        vBlockID.setValue(blockID);
        txUpdateBuilder.update();

        vAccTag.setValue(tag);
        vAccBlockID.setValue(blockID);
        txAccUpdateBuilder.update();
    }

    private BlockID intersect(Block aBlock, Block bBlock) throws SQLException {

        Objects.requireNonNull(aBlock);
        Objects.requireNonNull(bBlock);

        BlockID aBlockID = aBlock.getPreviousBlock();
        BlockID bBlockID = bBlock.getPreviousBlock();

        int aBlockHeight = aBlock.getHeight() - 1;
        int bBlockHeight = bBlock.getHeight() - 1;

        if (Math.abs(aBlockHeight - bBlockHeight) > Constant.BLOCK_IN_DAY) {
            throw new UnsupportedOperationException();
        }

        while (aBlockHeight > bBlockHeight) {
            aBlockID = getPreviousBlockId(aBlockID);
            aBlockHeight--;
            Objects.requireNonNull(aBlockID);
        }

        while (bBlockHeight > aBlockHeight) {
            bBlockID = getPreviousBlockId(bBlockID);
            bBlockHeight--;
            Objects.requireNonNull(bBlockID);
        }

        while (!aBlockID.equals(bBlockID)) {
            aBlockID = getPreviousBlockId(aBlockID);
            bBlockID = getPreviousBlockId(bBlockID);

            if (aBlockID == null || bBlockID == null) {
                throw new IllegalStateException();
            }
        }

        return bBlockID;
    }

    public Blockchain getBlockchain(Block block) {
        blockchainEventManager.raiseBlockchainChanging(this, block);
        // TODO: check that block is exists
        return new Blockchain(storage, block);
    }

    public Block setBlockchain(Blockchain blockchain) {
        return setLastBlock(blockchain.getLastBlock());
    }

    /**
     * Get transaction from forked blockchain and that not exist in main blockchain
     *
     * @param timestamp time limit for search
     * @return transaction list
     */
    private List<DbTransaction> getForkedTransactions(int timestamp) {

        try {

            if (forkedBuilder == null) {

                Dao<DbTransaction, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);

                // Ids of transactions in main blockchain
                QueryBuilder<DbTransaction, Long> subQueryBuilder = dao.queryBuilder();
                subQueryBuilder.selectColumns("id");
                Where<DbTransaction, Long> sw = subQueryBuilder.where();

                sw.gt("timestamp", vTimestampSub);
                sw.and().eq("tag", 1);

                // Forked transactions not in main blockchain
                forkedBuilder = dao.queryBuilder();
                Where<DbTransaction, Long> w = forkedBuilder.where();
                w.gt("timestamp", vTimestamp);
                w.and().eq("tag", 0);
                w.and().notIn("id", subQueryBuilder);
            }

            vTimestampSub.setValue(timestamp);
            vTimestamp.setValue(timestamp);

            return forkedBuilder.query();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    private void attachTo(Block milestone, Block newHead) throws SQLException {
        BlockID currentBlockID = newHead.getID();
        BlockID milestoneBlockID = milestone.getID();
        while (true) {
            attachBlock(currentBlockID);

            if (currentBlockID.equals(milestoneBlockID)) {
                break;
            }
            currentBlockID = getPreviousBlockId(currentBlockID);
            if (currentBlockID == null) {
                throw new IllegalStateException();
            }
        }
    }

    public Blockchain createBlockchain() {
        return new Blockchain(storage, null);
    }
}
