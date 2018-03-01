package com.exscudo.peer.core.blockchain;

import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.Callable;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.core.storage.utils.BlockHelper;
import com.exscudo.peer.core.storage.utils.BlockchainHelper;

public class Blockchain {
    private final Storage storage;
    private final int forkHeight;
    private final BlockchainHelper blockchainHelper;
    private final BlockID forkBlockID;
    private Block headBlock;
    private BlockHelper blockHelper;

    Blockchain(Storage storage, Block block) {
        this.storage = storage;
        this.headBlock = block;
        this.blockHelper = storage.getBlockHelper();
        this.blockchainHelper = storage.getBlockchainHelper();
        this.forkHeight = block.getHeight();
        this.forkBlockID = block.getID();
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

                Block block = blockHelper.get(newBlock.getID());
                if (block != null) {
                    headBlock = block;
                    return headBlock;
                }

                doSave(newBlock);
                headBlock = newBlock;
                return headBlock;
            }
        });
    }

    private void doSave(Block newBlock) throws SQLException {
        // TODO: check that throw exception if transaction exists.
        blockHelper.save(newBlock);
    }

    public Block getByHeight(int generationHeight) {
        // TODO: add cache
        try {

            // Fast search in main blockchain
            if (generationHeight < forkHeight) {
                Block targetBlock = blockchainHelper.getByHeight(generationHeight);
                if (blockchainHelper.getBlockHeight(forkBlockID) != -1) {
                    return targetBlock;
                }
            }

            // Search in forked blockchain
            BlockID currBlockID = headBlock.getPreviousBlock();
            int height = headBlock.getHeight() - 1;

            while (height > generationHeight && currBlockID != null) {

                currBlockID = blockHelper.getPreviousBlockId(currBlockID);
                height--;
            }

            if (currBlockID != null && height == generationHeight) {
                return blockHelper.get(currBlockID);
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

                if (blockHelper.containsTransaction(currBlockId, txID)) {
                    return true;
                }

                timestamp -= Constant.BLOCK_PERIOD;

                currBlockId = blockHelper.getPreviousBlockId(currBlockId);
                Objects.requireNonNull(currBlockId);
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }
}
