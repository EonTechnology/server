package com.exscudo.peer.core.backlog;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.blockchain.IBlockchainService;
import com.exscudo.peer.core.blockchain.TransactionProvider;
import com.exscudo.peer.core.blockchain.events.BlockEvent;
import com.exscudo.peer.core.blockchain.events.IBlockEventListener;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;

/**
 * Performs the task of removing expired transaction, duplicate transactions,
 * transactions with invalid signatures
 */
public class BacklogCleaner implements IBlockEventListener {
    private final IBlockchainService blockchain;

    private final IBacklogService backlog;
    private final TransactionProvider transactionProvider;
    private boolean logging = true;

    public BacklogCleaner(IBacklogService backlog,
                          IBlockchainService blockchain,
                          TransactionProvider transactionProvider) {

        this.backlog = backlog;
        this.blockchain = blockchain;
        this.transactionProvider = transactionProvider;
    }

    public void removeInvalidTransaction() {

        PrintWriter out = null;

        try {

            if (isLogging()) {
                out = new PrintWriter(new BufferedWriter(new FileWriter("removed_transaction.log", true)));
            }

            TreeSet<Transaction> trans = new TreeSet<>(new Comparator<Transaction>() {
                @Override
                public int compare(Transaction tx1, Transaction tx2) {
                    int res = Integer.compare(tx1.getTimestamp(), tx2.getTimestamp());
                    if (res == 0) {
                        res = Long.compare(tx1.getID().getValue(), tx2.getID().getValue());
                    }
                    return res;
                }
            });

            Block lastBlock = blockchain.getLastBlock();
            int timePeriod = lastBlock.getTimestamp() - Constant.SECONDS_IN_DAY;

            List<Transaction> forked = transactionProvider.getForkedTransactions(timePeriod);
            List<Transaction> copy = backlog.copyAndClear();

            trans.addAll(forked);
            trans.addAll(copy);

            // ATTENTION. Time of the last block is used as the current time.
            // Therefore the buffer of the Unconfirmed Transactions can
            // gradually be filled if the env is not involved in the network.
            // Because new blocks will no longer be created.

            for (Transaction tx : trans) {
                try {
                    backlog.put(tx);
                } catch (ValidateException e) {

                    if (isLogging()) {
                        out.print("ValidationError (" + e.getMessage() + "): ");
                        out.println(tx.toString());
                    }
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

    public boolean isLogging() {
        return logging;
    }

    public void setLogging(boolean logging) {
        this.logging = logging;
    }
}
