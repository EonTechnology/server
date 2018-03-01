package com.exscudo.peer.core.backlog.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.api.IBlockSynchronizationService;
import com.exscudo.peer.core.api.IMetadataService;
import com.exscudo.peer.core.api.SalientAttributes;
import com.exscudo.peer.core.backlog.IBacklogService;
import com.exscudo.peer.core.blockchain.IBlockchainService;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.common.exceptions.LifecycleException;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.env.Peer;
import com.exscudo.peer.core.importer.IFork;

/**
 * Performs the task of synchronizing the list of transactions from forked node.
 * <p>
 * The goal is to ensure the synchronization of transactions when forking the
 * network (on the case when somebody starts the network with changed blocks to
 * the maximum generation depth, which is given out to users as a real network).
 */
public final class SyncForkedTransactionListTask implements Runnable {

    private final IFork fork;
    private final ExecutionContext context;
    private final IBlockchainService blockchainService;
    private final TimeProvider timeProvider;
    private final IBacklogService backlogService;

    public SyncForkedTransactionListTask(IFork fork,
                                         ExecutionContext context,
                                         TimeProvider timeProvider,
                                         IBacklogService backlogService,
                                         IBlockchainService blockchainService) {
        this.fork = fork;
        this.context = context;
        this.blockchainService = blockchainService;
        this.timeProvider = timeProvider;
        this.backlogService = backlogService;
    }

    @Override
    public void run() {

        try {

            if (fork.isPassed(timeProvider.get())) {

                // If the current hard-fork time was passed, then the all of the
                // synchronization tasks would be stopped. The node needs to be
                // updated to the new version.
                return;
            }

            Block lastBlock = blockchainService.getLastBlock();
            if (timeProvider.get() < lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * 2 / 3) {

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
                    int forkNumber = fork.getNumber(lastBlock.getTimestamp());
                    if (attributes.getFork() == forkNumber) {

                        IBlockSynchronizationService blockService = peer.getBlockSynchronizationService();

                        Difficulty difficulty = blockService.getDifficulty();
                        if (blockchainService.getBlock(difficulty.getLastBlockID()) == null) {

                            Block remoteLastBlock = blockService.getLastBlock();
                            Collections.addAll(newTransactions,
                                               remoteLastBlock.getTransactions().toArray(new Transaction[0]));
                        }
                    }
                } catch (RemotePeerException | IOException e) {

                    context.disablePeer(peer);
                    Loggers.trace(SyncForkedTransactionListTask.class,
                                  "Failed to execute a request. Target: " + peer,
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

                        if (tx.isFuture(timeProvider.get() + Constant.MAX_LATENCY)) {
                            throw new LifecycleException();
                        }
                        backlogService.put(tx);
                    } catch (ValidateException e) {

                        Loggers.trace(SyncTransactionListTask.class,
                                      "Unable to process transaction passed from " + peer + ". " + tx.toString(),
                                      e);
                    }
                }
            }
        } catch (Exception e) {
            Loggers.error(SyncForkedTransactionListTask.class, e);
        }
    }
}
