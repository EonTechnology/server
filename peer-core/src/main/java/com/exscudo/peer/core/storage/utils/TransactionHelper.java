package com.exscudo.peer.core.storage.utils;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;

/**
 * Management transactions in DB
 */
public class TransactionHelper {

    private Dao<DbTransaction, Long> daoTransaction;

    private QueryBuilder<DbTransaction, Long> getBuilder;
    private QueryBuilder<DbTransaction, Long> existBuilder;
    private QueryBuilder<DbTransaction, Long> forkedBuilder;

    private ArgumentHolder vID = new ThreadLocalSelectArg();
    private ArgumentHolder vTimestamp = new ThreadLocalSelectArg();
    private ArgumentHolder vTimestampSub = new ThreadLocalSelectArg();

    public TransactionHelper(ConnectionSource connectionSource) {
        try {
            daoTransaction = DaoManager.createDao(connectionSource, DbTransaction.class);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Read transaction from DB
     *
     * @param id transaction id
     * @return transaction or null if transaction not exist
     * @throws SQLException problems with the DB
     */
    public Transaction get(final TransactionID id) throws SQLException {

        if (getBuilder == null) {
            getBuilder = daoTransaction.queryBuilder();
            getBuilder.where().eq("id", vID).and().eq("tag", 1);
        }

        vID.setValue(id.getValue());
        DbTransaction dbTx = getBuilder.queryForFirst();
        if (dbTx != null) {
            return dbTx.toTransaction();
        }
        return null;
    }

    /**
     * Check if transaction exists
     *
     * @param id transaction id
     * @return true if transaction exists, otherwise false
     * @throws SQLException problems with the DB
     */
    public boolean contains(TransactionID id) throws SQLException {

        if (existBuilder == null) {
            existBuilder = daoTransaction.queryBuilder();
            existBuilder.where().eq("id", vID).and().eq("tag", 1);
        }

        vID.setValue(id.getValue());
        long countOf = existBuilder.countOf();
        return (countOf != 0);
    }

    /**
     * Get transaction from forked blockchain and that not exist in main blockchain
     *
     * @param timestamp time limit for search
     * @return transaction list
     * @throws SQLException problems with the DB
     */
    public List<Transaction> getForked(int timestamp) throws SQLException {

        if (forkedBuilder == null) {
            // Ids of transactions in main blockchain
            QueryBuilder<DbTransaction, Long> subQueryBuilder = daoTransaction.queryBuilder();
            subQueryBuilder.selectColumns("id");
            Where<DbTransaction, Long> sw = subQueryBuilder.where();

            sw.gt("timestamp", vTimestampSub);
            sw.and().eq("tag", 1);

            // Forked transactions not in main blockchain
            forkedBuilder = daoTransaction.queryBuilder();
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
    }
}
