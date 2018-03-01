package com.exscudo.peer.core.api.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.api.ITransactionSynchronizationService;
import com.exscudo.peer.core.backlog.IBacklogService;
import com.exscudo.peer.core.blockchain.IBlockchainService;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.importer.IFork;

/**
 * Basic implementation of the {@code ITransactionSynchronizationService}
 * interface
 */
public class SyncTransactionService extends BaseService implements ITransactionSynchronizationService {
    /**
     * The maximum number of transactions transmitted during synchronization.
     */
    private static final int TRANSACTION_LIMIT = 100;

    private final IBlockchainService blockchainService;
    private final IFork fork;
    private final TimeProvider timeProvider;
    private final IBacklogService backlogService;

    public SyncTransactionService(IFork fork,
                                  TimeProvider timeProvider,
                                  IBacklogService backlogService,
                                  IBlockchainService blockchainService) {
        this.blockchainService = blockchainService;
        this.fork = fork;
        this.timeProvider = timeProvider;
        this.backlogService = backlogService;
    }

    @Override
    public Transaction[] getTransactions(String lastBlockId,
                                         String[] ignoreList) throws RemotePeerException, IOException {

        List<TransactionID> ignoreIDs = new ArrayList<>();
        try {

            if (!blockchainService.getLastBlock().getID().equals(new BlockID(lastBlockId)) &&
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

            final Iterator<TransactionID> indexes = backlogService.iterator();
            while (indexes.hasNext() && list.size() < TRANSACTION_LIMIT && blockSize > 0) {

                TransactionID id = indexes.next();

                if (!ignoreIDs.contains(id)) {
                    Transaction tx = backlogService.get(id);
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
