package com.exscudo.peer.eon.listeners;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.events.BlockEvent;
import com.exscudo.peer.core.events.IBlockEventListener;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.utils.Loggers;
import com.exscudo.peer.eon.Sandbox;

/**
 * Performs the task of removing expired transaction, duplicate transactions,
 * transactions with invalid signatures
 */
public class BacklogCleaner implements IBlockEventListener {
	private static final boolean LOGGING = true;

	private final IBacklogService backlog;
	private final IBlockchainService blockchain;

	public BacklogCleaner(IBacklogService backlog, IBlockchainService blockchain) {

		this.backlog = backlog;
		this.blockchain = blockchain;

	}

	public void removeInvalidTransaction() {

		PrintWriter out = null;
		try {

			// ATTENTION. Time of the last block is used as the current time.
			// Therefore the buffer of the Unconfirmed Transactions can
			// gradually be filled if the node is not involved in the network.
			// Because new blocks will no longer be created.

			Sandbox sandbox = Sandbox.getInstance(blockchain);

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
					sandbox.execute(tx);
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
	public void onBeforeChanging(BlockEvent event) {

	}

	@Override
	public void onLastBlockChanged(BlockEvent event) {
		removeInvalidTransaction();
	}
}
