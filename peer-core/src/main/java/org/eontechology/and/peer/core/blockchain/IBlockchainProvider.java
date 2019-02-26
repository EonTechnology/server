package org.eontechology.and.peer.core.blockchain;

import org.eontechology.and.peer.core.data.Block;
import org.eontechology.and.peer.core.data.identifier.BlockID;

/**
 * The {@code IBlockchainProvider} interface provides an abstraction for
 * accessing the chain of blocks.
 */
public interface IBlockchainProvider {

    /**
     * Returns the last block in the chain
     *
     * @return block
     */
    Block getLastBlock();

    /**
     * Returns a block with the specified {@code blockID}
     *
     * @param blockID
     * @return block or null
     */
    Block getBlock(BlockID blockID);

    /**
     * Returns a block by specified height.
     *
     * @return block or null
     */
    Block getBlockByHeight(int height);

    /**
     * Returns for the block specified by the {@code id} the index in the chain
     *
     * @param id
     * @return returns the height if the block is found, otherwise return -1
     */
    int getBlockHeight(BlockID id);

    /**
     * Finds the latest blocks in the chain.
     *
     * @param frameSize The number of returned units.
     * @return array of ids
     */
    BlockID[] getLatestBlocks(int frameSize);
}
