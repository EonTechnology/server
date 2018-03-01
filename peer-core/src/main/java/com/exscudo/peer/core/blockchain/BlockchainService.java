package com.exscudo.peer.core.blockchain;

import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.Callable;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.core.storage.utils.BlockHelper;
import com.exscudo.peer.core.storage.utils.BlockchainHelper;

public class BlockchainService implements IBlockchainService {
    private final Storage storage;
    private final BlockHelper blockHelper;
    private final BlockchainHelper blockchainHelper;

    private BlockID genesisBlockID;
    private volatile Block lastBlock;

    public BlockchainService(Storage storage) throws SQLException {
        this.storage = storage;
        this.blockHelper = storage.getBlockHelper();
        this.blockchainHelper = storage.getBlockchainHelper();
        initialize();
    }

    private void initialize() throws SQLException {
        Storage.Metadata metadata = storage.metadata();
        genesisBlockID = metadata.getGenesisBlockID();
        lastBlock = blockHelper.get(metadata.getLastBlockID());
    }

    public BlockID getGenesisBlockID() {
        return genesisBlockID;
    }

    @Override
    public synchronized Block getLastBlock() {
        // TODO: clone
        return lastBlock;
    }

    @Override
    public Block getBlock(BlockID blockID) {
        try {
            if (blockchainHelper.containBlock(blockID)) {
                return blockHelper.get(blockID);
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public Block getBlockByHeight(int height) {
        try {
            return blockchainHelper.getByHeight(height);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public int getBlockHeight(BlockID id) {
        try {
            return blockchainHelper.getBlockHeight(id);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public BlockID[] getLatestBlocks(int limit) {
        try {
            int height = lastBlock.getHeight();
            return blockchainHelper.getBlockLinkedList(height - limit + 1, height);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public synchronized Block setLastBlock(Block newLastBlock) {

        Difficulty diffNew = new Difficulty(newLastBlock);
        Difficulty currNew = new Difficulty(lastBlock);
        if (diffNew.compareTo(currNew) > 0) {
            storage.callInTransaction(new Callable<Void>() {
                @Override
                public Void call() throws Exception {

                    setPointerTo(newLastBlock);
                    storage.metadata().setLastBlockID(newLastBlock.getID());
                    lastBlock = newLastBlock;

                    return null;
                }
            });
        }

        return lastBlock;
    }

    private void setPointerTo(Block newHead) throws SQLException {

        BlockID milestoneBlockID = intersect(lastBlock, newHead);
        BlockID currentBlockID = lastBlock.getID();

        while (!currentBlockID.equals(milestoneBlockID)) {
            blockchainHelper.detachBlock(currentBlockID);
            currentBlockID = blockHelper.getPreviousBlockId(currentBlockID);
            if (currentBlockID == null) {
                throw new IllegalStateException();
            }
        }

        currentBlockID = newHead.getID();
        while (!currentBlockID.equals(milestoneBlockID)) {
            blockchainHelper.attachBlock(currentBlockID);
            currentBlockID = blockHelper.getPreviousBlockId(currentBlockID);
            if (currentBlockID == null) {
                throw new IllegalStateException();
            }
        }
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
            aBlockID = blockHelper.getPreviousBlockId(aBlockID);
            aBlockHeight--;
            Objects.requireNonNull(aBlockID);
        }

        while (bBlockHeight > aBlockHeight) {
            bBlockID = blockHelper.getPreviousBlockId(bBlockID);
            bBlockHeight--;
            Objects.requireNonNull(bBlockID);
        }

        while (!aBlockID.equals(bBlockID)) {
            aBlockID = blockHelper.getPreviousBlockId(aBlockID);
            bBlockID = blockHelper.getPreviousBlockId(bBlockID);

            if (aBlockID == null || bBlockID == null) {
                throw new IllegalStateException();
            }
        }

        return bBlockID;
    }

    public Blockchain getBlockchain(Block block) {
        // TODO: check that block is exists
        return new Blockchain(storage, block);
    }

    public Block setBlockchain(Blockchain blockchain) {
        return setLastBlock(blockchain.getLastBlock());
    }
}
