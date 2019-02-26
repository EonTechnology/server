package org.eontechology.and.eon.app.api.peer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eontechology.and.peer.core.Constant;
import org.eontechology.and.peer.core.IFork;
import org.eontechology.and.peer.core.api.ITransactionSynchronizationService;
import org.eontechology.and.peer.core.backlog.IBacklog;
import org.eontechology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechology.and.peer.core.common.ITimeProvider;
import org.eontechology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.BlockID;
import org.eontechology.and.peer.core.data.identifier.TransactionID;

/**
 * Basic implementation of the {@code ITransactionSynchronizationService}
 * interface
 */
public class SyncTransactionService extends BaseService implements ITransactionSynchronizationService {
    /**
     * The maximum number of transactions transmitted during synchronization.
     */
    private static final int TRANSACTION_LIMIT = 100;

    private final IBlockchainProvider blockchain;
    private final IFork fork;
    private final ITimeProvider timeProvider;
    private final IBacklog backlog;

    public SyncTransactionService(IFork fork,
                                  ITimeProvider timeProvider,
                                  IBacklog backlog,
                                  IBlockchainProvider blockchain) {
        this.blockchain = blockchain;
        this.fork = fork;
        this.timeProvider = timeProvider;
        this.backlog = backlog;
    }

    @Override
    public Transaction[] getTransactions(String lastBlockId,
                                         String[] ignoreList) throws RemotePeerException, IOException {

        List<TransactionID> ignoreIDs = new ArrayList<>();
        try {

            if (!blockchain.getLastBlock().getID().equals(new BlockID(lastBlockId)) &&
                    !fork.isPassed(timeProvider.get())) {
                return new Transaction[0];
            }

            for (String encodedID : ignoreList) {
                ignoreIDs.add(new TransactionID(encodedID));
            }
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException("Unsupported request. Invalid transaction ID format.", e);
        }

        try {

            List<Transaction> list = new ArrayList<>();

            int blockSize = Constant.BLOCK_TRANSACTION_LIMIT;

            final Iterator<TransactionID> indexes = backlog.iterator();
            while (indexes.hasNext() && list.size() < TRANSACTION_LIMIT && blockSize > 0) {

                TransactionID id = indexes.next();

                if (!ignoreIDs.contains(id)) {
                    Transaction tx = backlog.get(id);
                    if (tx != null) {
                        list.add(tx);
                    }
                }

                blockSize--;
            }
            return list.toArray(new Transaction[0]);
        } catch (Exception e) {
            throw new IOException("Failed to get the transaction list.", e);
        }
    }
}
