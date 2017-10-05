package com.exscudo.peer.core.services;

import java.io.IOException;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.core.exceptions.RemotePeerException;

/**
 * The protocol used to synchronize a block.
 *
 */
public interface IBlockSynchronizationService {

	/**
	 * Returns the current "difficulty" of the chain.
	 * 
	 * @return
	 * @throws RemotePeerException
	 *             An error in the protocol.
	 * @throws IOException
	 *             Error during access to the services node.
	 */
	Difficulty getDifficulty() throws RemotePeerException, IOException;

	/**
	 * Returns an array of the last blocks.
	 * 
	 * @param blockSequence
	 *            IDs of the last blocks.
	 * @return
	 * @throws RemotePeerException
	 *             An error in the protocol.
	 * @throws IOException
	 *             Error during access to the services node.
	 */
	Block[] getBlockHistory(String[] blockSequence) throws RemotePeerException, IOException;

	/**
	 * Returns last block
	 * 
	 * @return
	 * @throws RemotePeerException
	 *             An error in the protocol.
	 * @throws IOException
	 *             Error during access to the services node.
	 */
	Block getLastBlock() throws RemotePeerException, IOException;

}
