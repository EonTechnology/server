package org.eontechology.and.peer.core.backlog;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eontechology.and.peer.core.blockchain.events.BlockchainEvent;
import org.eontechology.and.peer.core.blockchain.events.IBlockchainEventListener;
import org.eontechology.and.peer.core.blockchain.events.UpdatedBlockchainEvent;
import org.eontechology.and.peer.core.common.Loggers;
import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Block;
import org.eontechology.and.peer.core.data.Transaction;

/**
 * Performs the task of removing expired transaction, duplicate transactions,
 * transactions with invalid signatures, etc.
 * <p>
 * In other words, re-import of transactions taking into account the new state.
 */
public class BacklogCleaner implements IBlockchainEventListener {

    private final Backlog backlog;

    private Thread thread = null;
    private SortedSet<Transaction> set = Collections.synchronizedSortedSet(new TreeSet<>(new TransactionComparator()));

    public BacklogCleaner(Backlog backlog) {

        this.backlog = backlog;
    }

    private synchronized void removeInvalidTransaction(Block lastBlock, List<Transaction> forked) {

        try {

            if (thread != null && thread.isAlive()) {
                thread.interrupt();
                thread.join();
            }

            List<Transaction> copy = backlog.copyAndClear();

            set.addAll(forked);
            set.addAll(copy);
            set.removeAll(lastBlock.getTransactions());

            if (!set.isEmpty()) {

                Runnable runnable = new CleanRunnable(set, backlog);

                // With a small number of transactions run in the current thread
                if (set.size() < 1000) {
                    runnable.run();
                } else {
                    thread = new Thread(runnable);
                    thread.setDaemon(true);
                    thread.start();
                }
            }
        } catch (Exception e) {
            Loggers.error(BacklogCleaner.class, e);
        }
    }

    // region IBlockchainEventListener members

    @Override
    public void onChanging(BlockchainEvent event) {

    }

    @Override
    public void onChanged(UpdatedBlockchainEvent event) {
        removeInvalidTransaction(event.block, event.forked);
    }

    //endregion

    private static class CleanRunnable implements Runnable {

        private final SortedSet<Transaction> set;
        private final Backlog backlog;

        CleanRunnable(SortedSet<Transaction> set, Backlog backlog) {

            this.set = set;
            this.backlog = backlog;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted() && !set.isEmpty()) {

                    Transaction first = set.first();
                    set.remove(first);
                    try {
                        backlog.put(first);
                    } catch (ValidateException e) {
                        Loggers.info(BacklogCleaner.class,
                                     String.format("ValidationError %s: %s (%s)",
                                                   first.getID(),
                                                   e.getMessage(),
                                                   first.toString()));
                    }
                }
            } catch (Throwable th) {
                Loggers.error(BacklogCleaner.class, th);
            }
        }
    }

    private static class TransactionComparator implements Comparator<Transaction> {

        @Override
        public int compare(Transaction tx1, Transaction tx2) {
            int res = Integer.compare(tx1.getTimestamp(), tx2.getTimestamp());
            if (res == 0) {
                res = Long.compare(tx1.getID().getValue(), tx2.getID().getValue());
            }
            return res;
        }
    }
}
