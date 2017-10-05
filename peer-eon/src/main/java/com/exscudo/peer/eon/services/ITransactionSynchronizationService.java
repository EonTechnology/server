package com.exscudo.peer.eon.services;

import java.io.IOException;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.RemotePeerException;

/**
 * The protocol used to synchronize a transactions.
 *
 */
public interface ITransactionSynchronizationService {

	/**
	 * Returns the list of Unconfirmed Transactions (currently not included in the
	 * block), except for the transactions which are specified in parameters.
	 *
	 * @param lastBlockId
	 *            last block ID. Sync only with the correct block ID.
	 * @param ignoreList
	 *            transaction IDs that exists at the node that makes a request.
	 * @return An array of transactions.
	 * @throws RemotePeerException
	 *             An error in the protocol. Invalid parameters are passed or an
	 *             error in the response format.
	 * @throws IOException
	 *             Error during access to the node.
	 */
	Transaction[] getTransactions(String lastBlockId, String[] ignoreList) throws RemotePeerException, IOException;

}
