package com.exscudo.peer.core.backlog;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.blockchain.events.BlockEvent;
import com.exscudo.peer.core.blockchain.events.IBlockEventListener;
import com.exscudo.peer.core.blockchain.storage.DbTransaction;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.storage.Storage;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import com.j256.ormlite.stmt.Where;

/**
 * Performs the task of removing expired transaction, duplicate transactions,
 * transactions with invalid signatures
 */
public class BacklogCleaner implements IBlockEventListener {

    private final Storage storage;
    private final IBacklog backlog;

    private Thread thread = null;
    private SortedSet<Transaction> set = Collections.synchronizedSortedSet(new TreeSet<>(new TransactionComparator()));

    // region Prepared Statement

    private QueryBuilder<DbTransaction, Long> forkedBuilder;
    private ArgumentHolder vTimestamp = new ThreadLocalSelectArg();
    private ArgumentHolder vTimestampSub = new ThreadLocalSelectArg();

    // endregion

    public BacklogCleaner(IBacklog backlog, Storage storage) {

        this.backlog = backlog;
        this.storage = storage;
    }

    private synchronized void removeInvalidTransaction(Block lastBlock) {

        try {

            if (thread != null && thread.isAlive()) {
                thread.interrupt();
                thread.join();
            }

            int timePeriod = lastBlock.getTimestamp() - Constant.SECONDS_IN_DAY;

            List<Transaction> forked = getForkedTransactions(timePeriod);
            List<Transaction> copy = backlog.copyAndClear();

            set.addAll(forked);
            set.addAll(copy);

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

    /**
     * Get transaction from forked blockchain and that not exist in main blockchain
     *
     * @param timestamp time limit for search
     * @return transaction list
     */
    private List<Transaction> getForkedTransactions(int timestamp) throws SQLException {

        try {

            if (forkedBuilder == null) {

                Dao<DbTransaction, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);

                // Ids of transactions in main blockchain
                QueryBuilder<DbTransaction, Long> subQueryBuilder = dao.queryBuilder();
                subQueryBuilder.selectColumns("id");
                Where<DbTransaction, Long> sw = subQueryBuilder.where();

                sw.gt("timestamp", vTimestampSub);
                sw.and().eq("tag", 1);

                // Forked transactions not in main blockchain
                forkedBuilder = dao.queryBuilder();
                Where<DbTransaction, Long> w = forkedBuilder.where();
                w.gt("timestamp", vTimestamp);
                w.and().eq("tag", 0);
                w.and().notIn("id", subQueryBuilder);
            }

            vTimestampSub.setValue(timestamp);
            vTimestamp.setValue(timestamp);

            List<DbTransaction> dbTransactions = forkedBuilder.query();

            // Convert List<DbTransaction> to List<Transaction>
            LinkedList<Transaction> res = new LinkedList<>();
            for (DbTransaction tx : dbTransactions) {
                res.add(tx.toTransaction());
            }

            return res;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public void onBeforeChanging(BlockEvent event) {

    }

    @Override
    public void onLastBlockChanged(BlockEvent event) {
        removeInvalidTransaction(event.block);
    }

    private static class CleanRunnable implements Runnable {

        private final SortedSet<Transaction> set;
        private final IBacklog backlog;

        public CleanRunnable(SortedSet<Transaction> set, IBacklog backlog) {

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
