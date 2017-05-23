package com.exscudo.eon.peer.contract;

import java.io.IOException;

import com.exscudo.eon.peer.data.Transaction;
import com.exscudo.eon.peer.exceptions.RemotePeerException;

/**
 * The protocol used to synchronize.
 *
 */
public interface DataSynchronizationService {

	/**
	 * Returns the list of Unconfirmed Transactions (currently not included in
	 * the block), except transactions which are specified in parameters.
	 * 
	 * @param ignoreList
	 *            Transaction IDs that exists at the node that makes a request.
	 * @return An array of transactions.
	 * @throws RemotePeerException
	 *             An error in the protocol. Invalid parameters are passed or an
	 *             error in the response format.
	 * @throws IOException
	 *             Error during access to the node.
	 */
	@MethodName()
	Transaction[] getTransactions(String[] ignoreList) throws RemotePeerException, IOException;

	/**
	 * Returns the current "difficulty" of the chain.
	 * 
	 * @return
	 * @throws RemotePeerException
	 *             An error in the protocol.
	 * @throws IOException
	 *             Error during access to the remote node.
	 */
	@MethodName()
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
	 *             Error during access to the remote node.
	 */
	@MethodName()
	TransportableBlock[] getLastBlocks(String[] blockSequence) throws RemotePeerException, IOException;

}
