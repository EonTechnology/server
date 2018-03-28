package com.exscudo.peer.core.blockchain;

import java.sql.SQLException;

import com.exscudo.peer.core.blockchain.storage.DbTransaction;
import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.storage.Storage;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;

/**
 * Provides access to the transactions of the primary chain.
 */
public class TransactionProvider {
    private final Storage storage;

    private QueryBuilder<DbTransaction, Long> getBuilder;
    private QueryBuilder<DbTransaction, Long> existBuilder;
    private ArgumentHolder vID = new ThreadLocalSelectArg();

    public TransactionProvider(Storage storage) {
        this.storage = storage;
    }

    /**
     * Check if transaction exists
     *
     * @param id transaction id
     * @return true if transaction exists, otherwise false
     */
    public boolean containsTransaction(TransactionID id) {
        try {

            if (existBuilder == null) {
                Dao dao = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);
                existBuilder = dao.queryBuilder();
                existBuilder.where().eq("id", vID).and().eq("tag", 1);
            }

            vID.setValue(id.getValue());
            long countOf = existBuilder.countOf();
            return (countOf != 0);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    /**
     * Returns transaction with specified {@code id}
     *
     * @param id transaction id
     * @return transaction or null, if transaction not exist
     */
    public Transaction getTransaction(TransactionID id) {
        try {

            if (getBuilder == null) {
                Dao dao = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);
                getBuilder = dao.queryBuilder();
                getBuilder.where().eq("id", vID).and().eq("tag", 1);
            }

            vID.setValue(id.getValue());
            DbTransaction dbTx = getBuilder.queryForFirst();
            if (dbTx != null) {
                return dbTx.toTransaction();
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }
}
