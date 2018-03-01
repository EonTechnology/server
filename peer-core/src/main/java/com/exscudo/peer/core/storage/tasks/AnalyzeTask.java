package com.exscudo.peer.core.storage.tasks;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.blockchain.BlockchainService;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.storage.Storage;

/**
 * Management of top-level transaction in DB.
 * <p>
 * SQLite speed up when transaction active.
 */
public class AnalyzeTask implements Runnable {
    private final BlockchainService currentBlockchain;
    private final Storage storage;

    private BlockID lastBlockID = new BlockID(0L);

    public AnalyzeTask(BlockchainService currentBlockchain, Storage storage) {
        this.currentBlockchain = currentBlockchain;
        this.storage = storage;
    }

    @Override
    public void run() {

        try {

            Block lastBlock = currentBlockchain.getLastBlock();
            if (!lastBlockID.equals(lastBlock.getID())) {

                int time = (int) ((System.currentTimeMillis() + 500L) / 1000L);
                if (time > lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * 2 / 3) {
                    storage.analyze();
                    lastBlockID = lastBlock.getID();
                }
            }
        } catch (Exception e) {
            Loggers.error(AnalyzeTask.class, "Unable to perform task.", e);
        }
    }
}
