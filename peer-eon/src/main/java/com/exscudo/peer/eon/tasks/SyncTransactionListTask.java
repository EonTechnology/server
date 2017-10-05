package com.exscudo.peer.eon.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.LifecycleException;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.core.utils.Loggers;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.Instance;
import com.exscudo.peer.eon.Peer;
import com.exscudo.peer.eon.services.ITransactionSynchronizationService;

/**
 * Performs the task of synchronizing the list of unconfirmed transactions of
 * the current node and random connected node
 * <p>
 * Synchronization of transactions is performed only if the node is in the
 * actual state, i.e. the current hard-fork (see {@link Fork}) time has not
 * expired. In the first step, a random connected node for interaction is
 * selected. The randomly node selection algorithm is described in
 * {@code AbstractContext} implementation (see
 * {@link com.exscudo.peer.eon.ExecutionContext#getAnyConnectedPeer}). Next, a
 * list of transactions, which falls into the next block on the current node, is
 * sent to the services node (which selected on first step). The services node
 * compares the resulting transaction list to its own and returns the missing
 * transactions. At the end, the received transactions are added to the Backlog
 * (see {@link com.exscudo.peer.core.services.IBacklogService }) list.
 */
public final class SyncTransactionListTask extends BaseTask implements Runnable {

	public SyncTransactionListTask(ExecutionContext context) {
		super(context);
	}

	@Override
	public void run() {

		try {

			if (context.isCurrentForkPassed()) {

				// If the current hard-fork time was passed, then the all of the
				// synchronization tasks would be stopped. The node needs to be
				// updated to the new version.
				return;

			}

			Instance instance = context.getInstance();
			Peer peer = context.getAnyConnectedPeer();
			if (peer != null) {

				Transaction[] newTransactions = null;
				try {

					ArrayList<String> encodedIDs = new ArrayList<>();

					Iterator<Long> indexes = instance.getBacklogService().iterator();
					while (indexes.hasNext() && encodedIDs.size() < EonConstant.BLOCK_TRANSACTION_LIMIT) {
						Long id = indexes.next();
						encodedIDs.add(Format.ID.transactionId(id));
					}

					Block lastBlock = instance.getBlockchainService().getLastBlock();
					String lastBlockID = Format.ID.blockId(lastBlock.getID());

					ITransactionSynchronizationService service = peer.getTransactionSynchronizationService();
					newTransactions = service.getTransactions(lastBlockID, encodedIDs.toArray(new String[0]));

				} catch (RemotePeerException | IOException e) {

					context.disablePeer(peer);

					Loggers.trace(SyncTransactionListTask.class, "Failed to execute a request. Target: " + peer, e);
					Loggers.info(SyncTransactionListTask.class, "The node is disconnected. \"{}\".", peer);
					return;
				}

				// Performed consecutive import of transactions received from a
				// services node to the Backlog list.
				if (newTransactions != null) {

					for (Transaction tx : newTransactions) {

						if (tx == null) {
							continue;
						}
						try {

							if (tx.isFuture(context.getCurrentTime() + Constant.MAX_LATENCY)) {
								throw new LifecycleException();
							}
							instance.getBacklogService().put(tx);

						} catch (ValidateException e) {

							Loggers.trace(SyncTransactionListTask.class,
									"Unable to process transaction passed from " + peer + ". " + tx.toString(), e);

						}

					}
				}
			}

		} catch (Exception e) {
			Loggers.error(SyncTransactionListTask.class, e);
		}
	}

}
