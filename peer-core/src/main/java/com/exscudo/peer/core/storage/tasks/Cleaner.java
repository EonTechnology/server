package com.exscudo.peer.core.storage.tasks;

import java.sql.SQLException;
import java.util.List;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.blockchain.IBlockchainService;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.storage.Storage;

// TODO: add blockchain cleaner task: block, transactions

/**
 * Performs the task of removing side chains.
 */
public class Cleaner implements Runnable {

    private final Storage storage;
    private final IBlockchainService blockchainService;

    public Cleaner(IBlockchainService blockchainService, Storage storage) {

        this.blockchainService = blockchainService;
        this.storage = storage;
    }

    @Override
    public void run() {

        try {
            cleanTransactionAndBlock();
        } catch (Throwable e) {
            Loggers.error(Cleaner.class, "Unable to perform task.", e);
        }
    }

    private void cleanTransactionAndBlock() throws SQLException {

        Block lastBlock = blockchainService.getLastBlock();
        int timestamp = lastBlock.getTimestamp() - Constant.SECONDS_IN_DAY;

        List<BlockID> blocksIds = storage.getBlockHelper().getForkedBlocksIds(timestamp);
        for (BlockID id : blocksIds) {
            storage.getBlockHelper().remove(id);
        }
    }
}
