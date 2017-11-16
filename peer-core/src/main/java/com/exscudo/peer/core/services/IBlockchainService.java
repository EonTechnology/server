package com.exscudo.peer.core.services;

import com.exscudo.peer.core.data.Block;

/**
 * The {@code IBlockchainService} interface provides an abstraction for
 * accessing the chain of blocks.
 *
 */
public interface IBlockchainService {

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
	Block getBlock(long blockID);

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
	int getBlockHeight(long id);

	/**
	 * Finds the latest blocks in the chain.
	 * 
	 * @param frameSize
	 *            The number of returned units.
	 * @return array of ids
	 */
	long[] getLatestBlocks(int frameSize);

	/**
	 * Creates a unit of work for changing the chain of blocks
	 * 
	 * @param source
	 *            of change
	 * @param block
	 *            used as a head
	 * @return
	 */
	IUnitOfWork beginPush(Object source, Block block);

	/**
	 * Returns an object to access transactions in blocks
	 * 
	 * @return
	 */
	ITransactionMapper transactionMapper();

	/**
	 * Returns the status of accounts corresponding to the specified snapshot ID.
	 *
	 * @param snapshot
	 *            id
	 * @return {@code ILedger} object or null
	 */
	ILedger getState(byte[] snapshot);
}
