package com.exscudo.peer.core.backlog.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.api.ITransactionSynchronizationService;
import com.exscudo.peer.core.backlog.IBacklog;
import com.exscudo.peer.core.blockchain.IBlockchainProvider;
import com.exscudo.peer.core.common.ITimeProvider;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.env.Peer;

/**
 * Performs the task of synchronizing the list of unconfirmed transactions of
 * the current node and random connected node
 * <p>
 * Synchronization of transactions is performed only if the node is in the
 * actual state, i.e. the current hard-fork (see {@link IFork}) time has not
 * expired. In the first step, a random connected node for interaction is
 * selected. The randomly node selection algorithm is described in
 * {@code ExecutionContext} implementation (see
 * {@link ExecutionContext#getAnyConnectedPeer}). Next, a
 * list of transactions, which falls into the next block on the current node, is
 * sent to the services node (which selected on first step). The services node
 * compares the resulting transaction list to its own and returns the missing
 * transactions. At the end, the received transactions are added to the Backlog
 * (see {@link IBacklog }) list.
 */
public final class SyncTransactionListTask implements Runnable {

    private final ExecutionContext context;
    private final ITimeProvider timeProvider;
    private final IFork fork;
    private final IBlockchainProvider blockchainService;
    private final IBacklog backlogService;

    public SyncTransactionListTask(IFork fork,
                                   ExecutionContext context,
                                   ITimeProvider timeProvider,
                                   IBacklog backlogService,
                                   IBlockchainProvider blockchainService) {
        this.context = context;
        this.timeProvider = timeProvider;
        this.fork = fork;
        this.blockchainService = blockchainService;
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

            Peer peer = context.getAnyConnectedPeer();
            if (peer != null) {

                Transaction[] newTransactions = null;
                try {

                    ArrayList<String> encodedIDs = new ArrayList<>();

                    Iterator<TransactionID> indexes = backlogService.iterator();
                    while (indexes.hasNext() && encodedIDs.size() < Constant.BLOCK_TRANSACTION_LIMIT) {
                        TransactionID id = indexes.next();
                        encodedIDs.add(id.toString());
                    }

                    Block lastBlock = blockchainService.getLastBlock();
                    String lastBlockID = lastBlock.getID().toString();

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

                            backlogService.put(tx);
                        } catch (ValidateException e) {

                            Loggers.trace(SyncTransactionListTask.class,
                                          "Unable to process transaction passed from " + peer + ". " + tx.toString(),
                                          e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Loggers.error(SyncTransactionListTask.class, e);
        }
    }
}
