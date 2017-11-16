package com.exscudo.peer.eon.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.LifecycleException;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IBlockSynchronizationService;
import com.exscudo.peer.core.utils.Loggers;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.Instance;
import com.exscudo.peer.eon.Peer;
import com.exscudo.peer.eon.services.IMetadataService;
import com.exscudo.peer.eon.services.SalientAttributes;

/**
 * Performs the task of synchronizing the list of transactions from forked node.
 * <p>
 * The goal is to ensure the synchronization of transactions when forking the
 * network (on the case when somebody starts the network with changed blocks to
 * the maximum generation depth, which is given out to users as a real network).
 */
public final class SyncForkedTransactionListTask extends BaseTask implements Runnable {

	public SyncForkedTransactionListTask(ExecutionContext context) {
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
			Block lastBlock = instance.getBlockchainService().getLastBlock();
			if (context.getCurrentTime() < lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * 2 / 3) {

				// The task has a minimum priority. And it is executed, when the
				// main part of the synchronization tasks has been executed.
				return;
			}

			Peer peer = context.getAnyDisabledPeer();
			if (peer != null) {

				List<Transaction> newTransactions = new ArrayList<>();
				try {

					IMetadataService metadataService = peer.getMetadataService();

					SalientAttributes attributes = metadataService.getAttributes();
					int forkNumber = context.getCurrentFork().getNumber(lastBlock.getTimestamp());
					if (attributes.getFork() == forkNumber && forkNumber != -1) {

						IBlockSynchronizationService blockService = peer.getBlockSynchronizationService();

						Difficulty difficulty = blockService.getDifficulty();
						if (instance.getBlockchainService().getBlock(difficulty.getLastBlockID()) == null) {

							Block remoteLastBlock = blockService.getLastBlock();
							Collections.addAll(newTransactions,
									remoteLastBlock.getTransactions().toArray(new Transaction[0]));
						}
					}

				} catch (RemotePeerException | IOException e) {

					context.disablePeer(peer);
					Loggers.trace(SyncForkedTransactionListTask.class, "Failed to execute a request. Target: " + peer,
							e);
					Loggers.info(SyncForkedTransactionListTask.class, "The node is disconnected. \"{}\".", peer);
					return;

				}

				// Try to import a transactions received from a services node to
				// the Backlog list.
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

		} catch (Exception e) {
			Loggers.error(SyncForkedTransactionListTask.class, e);
		}
	}

}
