package com.exscudo.peer.core.api.impl;

import java.io.IOException;
import java.util.ArrayList;

import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.api.IBlockSynchronizationService;
import com.exscudo.peer.core.blockchain.IBlockchainService;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.env.ExecutionContext;

/**
 * Basic implementation of the {@code IBlockSynchronizationService} interface
 */
public class SyncBlockService extends BaseService implements IBlockSynchronizationService {
    /**
     * The maximum number of blocks transmitted during synchronization.
     */
    public static final int BLOCK_LIMIT = 10;

    private final ExecutionContext context;
    private final IBlockchainService blockchainService;

    public SyncBlockService(ExecutionContext context, IBlockchainService blockchainService) {
        this.context = context;
        this.blockchainService = blockchainService;
    }

    @Override
    public Difficulty getDifficulty() throws RemotePeerException, IOException {
        Block lastBlock = blockchainService.getLastBlock();
        return new Difficulty(lastBlock.getID(), lastBlock.getCumulativeDifficulty());
    }

    @Override
    public Block[] getBlockHistory(String[] blockSequence) throws RemotePeerException, IOException {

        BlockID lastBlockID = blockchainService.getLastBlock().getID();
        BlockID commonBlockID = null;

        try {

            int commonBlockHeight = -1;
            for (String encodedID : blockSequence) {
                BlockID id = new BlockID(encodedID);
                int height = blockchainService.getBlockHeight(id);
                if (height > commonBlockHeight) {
                    commonBlockHeight = height;
                    commonBlockID = id;
                }
            }
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException("Unsupported request. Invalid block ID format.", e);
        }

        if (!lastBlockID.equals(blockchainService.getLastBlock().getID())) {
            throw new RemotePeerException("Last block changed");
        }

        if (commonBlockID != null) {

            ArrayList<Block> nextBlocks = new ArrayList<>();
            BlockID id = commonBlockID;
            Block block = blockchainService.getBlock(id);
            while (nextBlocks.size() < BLOCK_LIMIT && block != null) {

                block = blockchainService.getBlockByHeight(block.getHeight() + 1);
                if (block == null || !block.getPreviousBlock().equals(id)) {
                    break;
                }

                id = block.getID();
                nextBlocks.add(block);

//                if (!lastBlockID.equals(blockchainService.getLastBlock().getID())) {
//                    break;
//                }
            }
            return nextBlocks.toArray(new Block[0]);
        }

        return new Block[0];
    }

    @Override
    public Block getLastBlock() throws RemotePeerException, IOException {
        return blockchainService.getLastBlock();
    }
}
