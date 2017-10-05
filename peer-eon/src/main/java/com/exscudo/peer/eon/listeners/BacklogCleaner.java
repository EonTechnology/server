package com.exscudo.peer.eon.listeners;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.events.IPeerEventListener;
import com.exscudo.peer.core.events.PeerEvent;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.*;
import com.exscudo.peer.core.utils.Loggers;

/**
 * Performs the task of removing expired transaction, duplicate transactions,
 * transactions with invalid signatures
 *
 */
public class BacklogCleaner implements IPeerEventListener {
	private static final boolean LOGGING = true;

	private final IBacklogService backlog;
	private final IBlockchainService blockchain;
	private final ITransactionHandler validator;

	public BacklogCleaner(IBacklogService backlog, IBlockchainService blockchain, ITransactionHandler validator) {

		this.backlog = backlog;
		this.blockchain = blockchain;

		this.validator = validator;
	}

	public void removeInvalidTransaction() {

		PrintWriter out = null;
		try {

			// ATTENTION. Time of the last block is used as the current time.
			// Therefore the buffer of the Unconfirmed Transactions can
			// gradually be filled if the node is not involved in the network.
			// Because new blocks will no longer be created.
			int curTime = blockchain.getLastBlock().getTimestamp();

			TransactionContext ctx = new TransactionContext(curTime);
			ISandbox sandbox = blockchain.getLastBlock().createSandbox(validator);

			Iterator<Long> indexes = backlog.iterator();
			while (indexes.hasNext()) {
				Long id = indexes.next();

				Transaction tx = backlog.get(id);
				if (tx == null) {
					continue;
				}

				// deleting old transactions, transactions with the wrong
				// signature and duplicate transactions.
				boolean duplicate = blockchain.transactionMapper().containsTransaction(id);

				try {
					sandbox.execute(tx, ctx);
				} catch (ValidateException e) {

					if (!duplicate && LOGGING) {
						if (out == null) {
							out = new PrintWriter(new BufferedWriter(new FileWriter("removed_transaction.log", true)));
						}
						out.print("ValidationError (" + e.getMessage() + "): ");
						out.println(tx.toString());
					}
					backlog.remove(id);

				}

			}

		} catch (Exception e) {
			Loggers.error(BacklogCleaner.class, e);
		} finally {
			if (out != null) {

				out.close();
			}
		}
	}

	@Override
	public void onSynchronized(PeerEvent event) {
		removeInvalidTransaction();
	}

}
