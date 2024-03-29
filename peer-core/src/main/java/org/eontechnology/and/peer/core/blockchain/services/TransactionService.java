package org.eontechnology.and.peer.core.blockchain.services;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eontechnology.and.peer.core.blockchain.storage.DbAccTransaction;
import org.eontechnology.and.peer.core.blockchain.storage.DbTransaction;
import org.eontechnology.and.peer.core.blockchain.storage.converters.DTOConverter;
import org.eontechnology.and.peer.core.common.exceptions.DataAccessException;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.data.identifier.TransactionID;
import org.eontechnology.and.peer.core.storage.Storage;

public class TransactionService {

  private static final int TRANSACTION_PAGE_SIZE = 20;

  private final Storage storage;

  private QueryBuilder<DbTransaction, Long> getPageBuilder = null;
  private QueryBuilder<DbAccTransaction, Long> getPageBuilderSubQ = null;
  private ArgumentHolder vAccount = new ThreadLocalSelectArg();

  private QueryBuilder<DbTransaction, Long> getByBlockBuilder = null;
  private ArgumentHolder vBlockID = new ThreadLocalSelectArg();

  private QueryBuilder<DbTransaction, Long> getByIdBuilder;
  private ArgumentHolder vTransactionID = new ThreadLocalSelectArg();

  public TransactionService(Storage storage) {
    this.storage = storage;
  }

  public List<Transaction> getByAccountId(AccountID accountID, int page) {
    return getPage(accountID, page * TRANSACTION_PAGE_SIZE, TRANSACTION_PAGE_SIZE);
  }

  /**
   * Find all transactions for account
   *
   * @param accountId account id
   * @return transaction map. Empty if user does not exist or has not sent any transaction.
   * @throws SQLException problems with the DB
   */
  public List<Transaction> getPage(AccountID accountId, long from, int limit) {

    try {

      if (getPageBuilder == null) {

        Dao<DbTransaction, Long> dao =
            DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);
        Dao<DbAccTransaction, Long> daoAcc =
            DaoManager.createDao(storage.getConnectionSource(), DbAccTransaction.class);

        getPageBuilderSubQ = daoAcc.queryBuilder();
        getPageBuilderSubQ
            .selectColumns("transaction_id")
            .where()
            .eq("tag", 1)
            .and()
            .eq("account_id", vAccount);
        getPageBuilderSubQ.orderBy("timestamp", false);

        getPageBuilder = dao.queryBuilder();
        getPageBuilder.where().in("id", getPageBuilderSubQ).and().eq("tag", 1);
        getPageBuilder.orderBy("timestamp", false);
      }

      vAccount.setValue(accountId.getValue());
      getPageBuilderSubQ.offset(from).limit((long) limit);

      return convert(getPageBuilder.query());
    } catch (SQLException e) {
      throw new DataAccessException();
    }
  }

  public Collection<Transaction> getByBlockId(BlockID blockID) {

    try {
      if (getByBlockBuilder == null) {

        Dao<DbTransaction, Long> dao =
            DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);

        getByBlockBuilder = dao.queryBuilder();
        getByBlockBuilder.where().eq("block_id", vBlockID);
      }

      vBlockID.setValue(blockID.getValue());
      return convert(getByBlockBuilder.query());
    } catch (SQLException e) {
      throw new DataAccessException();
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
      Transaction transaction = DTOConverter.convert(dbt);
      l.add(transaction);
    }
    return l;
  }

  public Transaction getById(TransactionID transactionID) {

    try {

      if (getByIdBuilder == null) {

        Dao<DbTransaction, Long> dao =
            DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);

        getByIdBuilder = dao.queryBuilder();
        getByIdBuilder.where().eq("id", vTransactionID).and().eq("tag", 1);
      }

      vTransactionID.setValue(transactionID.getValue());

      DbTransaction dbTx = getByIdBuilder.queryForFirst();
      if (dbTx != null) {
        return DTOConverter.convert(dbTx);
      }

      return null;
    } catch (SQLException e) {
      throw new DataAccessException();
    }
  }
}
