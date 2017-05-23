package com.exscudo.eon.peer.tasks;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Enumeration;

import com.exscudo.eon.StringConstant;
import com.exscudo.eon.peer.ExecutionContext;
import com.exscudo.eon.peer.data.Block;
import com.exscudo.eon.peer.data.DatastoreConnector;
import com.exscudo.eon.peer.data.DatastoreConnector.TransactionMapper;
import com.exscudo.eon.peer.data.Transaction;
import com.exscudo.eon.utils.Loggers;

/**
 * Performs the task of removing expired transaction, duplicate transactions,
 * transactions with invalid signatures
 *
 */
public final class InvalidTransactionRemoveTask extends AbstractTask implements Runnable {
	private static final boolean LOGGING = true;
	private final DatastoreConnector target;

	public InvalidTransactionRemoveTask(ExecutionContext context, DatastoreConnector target) {
		super(context);

		this.target = target;
	}

	@Override
	public void run() {

		PrintWriter out = null;
		try {

			Block lastBlock = target.blocks().getLastBlock();
			if (lastBlock == null) {

				// Blockchain is loading now...
				return;
			}

			// Time the last block is used as the current time. Therefore the
			// buffer of the Unconfirmed Transactions can gradually be filled if
			// the node is not involved in the network. Because the new blocks
			// will no longer be created.
			int curTime = lastBlock.getTimestamp();

			// Lock the transactions changing. Otherwise, if you update a tail
			// of the chain, the some transactions can be removed.

			synchronized (target.transactions().syncObject()) {

				final TransactionMapper txMapper = target.transactions();
				Enumeration<Long> indexes = txMapper.unconfirmed().indexes();
				while (indexes.hasMoreElements()) {
					long id = indexes.nextElement();

					Transaction tx = txMapper.unconfirmed().get(id);
					if (tx == null) {
						continue;
					}

					// deleting old transactions, transactions with the wrong
					// signature and duplicate transactions.
					boolean duplicate = (txMapper.confirmed().get(id) != null);
					if (duplicate || tx.isExpired(curTime) || !Transaction.validateSignature(tx)) {

						if (!duplicate && LOGGING) {
							if (out == null) {
								out = new PrintWriter(new BufferedWriter(
										new FileWriter(StringConstant.removedTransactionsFile, true)));
							}
							out.println(tx.toString());
						}

						target.removeTransaction(id);
					}

				}
			}

		} catch (Exception e) {
			Loggers.NOTICE.error(InvalidTransactionRemoveTask.class, e);
		} finally {
			if (out != null) {

				out.close();
			}
		}

	}
}
