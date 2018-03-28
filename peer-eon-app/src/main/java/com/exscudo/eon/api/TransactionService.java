package com.exscudo.eon.api;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.exscudo.peer.core.blockchain.storage.DbTransaction;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.eon.TransactionType;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import com.j256.ormlite.stmt.Where;

public class TransactionService {

    private final static int TRANSACTION_PAGE_SIZE = 20;

    private final Storage storage;

    private QueryBuilder<DbTransaction, Long> getRegistrationBuilder = null;
    private ArgumentHolder vAccount = new ThreadLocalSelectArg();

    private QueryBuilder<DbTransaction, Long> getPageBuilder = null;
    private ArgumentHolder vRecipient = new ThreadLocalSelectArg();
    private ArgumentHolder vSender = new ThreadLocalSelectArg();

    private QueryBuilder<DbTransaction, Long> getByBlockBuilder = null;
    private ArgumentHolder vBlockID = new ThreadLocalSelectArg();

    private QueryBuilder<DbTransaction, Long> getByIdBuilder;
    private ArgumentHolder vTransactionID = new ThreadLocalSelectArg();

    public TransactionService(Storage storage) {
        this.storage = storage;
    }

    /**
     * Get transaction of account registration
     *
     * @param id friendly account ID
     * @return Transaction of account registration if exists, else null
     * @throws RemotePeerException
     * @throws IOException
     */
    public Transaction getRegistration(String id) throws RemotePeerException, IOException {

        try {

            AccountID accountID = new AccountID(id);

            if (getRegistrationBuilder == null) {
                Dao dao = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);
                getRegistrationBuilder = dao.queryBuilder();

                Where<DbTransaction, Long> where = getRegistrationBuilder.where();
                where.eq("tag", 1).and().eq("type", TransactionType.Registration).and().eq("recipient_id", vAccount);
            }

            vAccount.setValue(accountID.getValue());

            DbTransaction dbt = getRegistrationBuilder.queryForFirst();
            if (dbt == null) {
                return null;
            }
            return dbt.toTransaction();
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        } catch (Exception e) {

            Loggers.error(TransactionService.class, e);
            throw new RemotePeerException();
        }
    }

    public List<Transaction> getByAccountId(String id, int page) throws RemotePeerException, IOException {
        try {
            AccountID accountID = new AccountID(id);
            return getPage(accountID, page * TRANSACTION_PAGE_SIZE, TRANSACTION_PAGE_SIZE);
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        } catch (Exception e) {
            Loggers.error(TransactionService.class, e);
            throw new RemotePeerException();
        }
    }

    /**
     * Find all transactions for account
     *
     * @param accountId account id
     * @return transaction map. Empty if user does not exist or has not sent any
     * transaction.
     * @throws SQLException problems with the DB
     */
    public List<Transaction> getPage(AccountID accountId, long from, int limit) throws SQLException {

        if (getPageBuilder == null) {

            Dao dao = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);

            getPageBuilder = dao.queryBuilder();
            Where<DbTransaction, Long> w = getPageBuilder.where();
            w.and(w.eq("tag", 1), w.or(w.eq("recipient_id", vRecipient), w.eq("sender_id", vSender)));
            getPageBuilder.orderBy("timestamp", false);
        }

        vRecipient.setValue(accountId.getValue());
        vSender.setValue(accountId.getValue());

        getPageBuilder.offset(from).limit((long) limit);

        return convert(getPageBuilder.query());
    }

    public Collection<Transaction> getByBlockId(String id) throws RemotePeerException, IOException {

        try {

            BlockID blockID = new BlockID(id);

            if (getByBlockBuilder == null) {

                Dao dao = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);

                getByBlockBuilder = dao.queryBuilder();
                getByBlockBuilder.where().eq("block_id", vBlockID);
            }

            vBlockID.setValue(blockID.getValue());
            return convert(getByBlockBuilder.query());
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        } catch (Exception e) {
            Loggers.error(TransactionService.class, e);
            throw new RemotePeerException();
        }
    }

    /**
     * Read all transactions from list
     *
     * @param list to convert
     * @return transaction list
     */
    private List<Transaction> convert(List<DbTransaction> list) {

        List<Transaction> l = new ArrayList<>();
        for (DbTransaction dbt : list) {
            Transaction transaction = dbt.toTransaction();
            l.add(transaction);
        }
        return l;
    }

    public Transaction getById(String id) throws RemotePeerException, IOException {

        try {

            TransactionID transactionID = new TransactionID(id);
            if (getByIdBuilder == null) {

                Dao dao = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);

                getByIdBuilder = dao.queryBuilder();
                getByIdBuilder.where().eq("id", vTransactionID).and().eq("tag", 1);
            }

            vTransactionID.setValue(transactionID.getValue());

            DbTransaction dbTx = getByIdBuilder.queryForFirst();
            if (dbTx != null) {
                return dbTx.toTransaction();
            }

            return null;
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        } catch (Exception e) {
            Loggers.error(TransactionService.class, e);
            throw new RemotePeerException();
        }
    }
}
