package org.eontechnology.and.peer.core.blockchain.services;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eontechnology.and.peer.core.blockchain.storage.DbBlock;
import org.eontechnology.and.peer.core.blockchain.storage.DbTransaction;
import org.eontechnology.and.peer.core.blockchain.storage.converters.DTOConverter;
import org.eontechnology.and.peer.core.common.exceptions.DataAccessException;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.data.identifier.TransactionID;
import org.eontechnology.and.peer.core.storage.Storage;

public class BlockService {
  private static final int BLOCK_LAST_PAGE_SIZE = 10;

  private final Storage storage;

  private QueryBuilder<DbBlock, Long> getPageBuilder = null;
  private ArgumentHolder vPageBegin = new ThreadLocalSelectArg();
  private ArgumentHolder vPageEnd = new ThreadLocalSelectArg();

  private QueryBuilder<DbBlock, Long> getByHeightBuilder = null;
  private ArgumentHolder vHeight = new ThreadLocalSelectArg();

  private QueryBuilder<DbBlock, Long> getByIdBuilder = null;
  private ArgumentHolder vBlockID = new ThreadLocalSelectArg();

  private QueryBuilder<DbBlock, Long> getByAccountIdBuilder = null;
  private ArgumentHolder vAccountID = new ThreadLocalSelectArg();

  private QueryBuilder<DbBlock, Long> getBlockByTxIdBuilder;
  private ArgumentHolder vTransactionID = new ThreadLocalSelectArg();

  public BlockService(Storage storage) {
    this.storage = storage;
  }

  public List<Block> getLastPage() {
    Block lastBlock = getLastBlock();
    return getPage(lastBlock.getHeight());
  }

  public List<Block> getPage(int height) {

    int end = height;
    int begin = height - BLOCK_LAST_PAGE_SIZE + 1;

    if (begin < 0) {
      begin = 0;
    }
    if (end < 0) {
      end = 0;
    }

    ArrayList<Block> blocks = new ArrayList<>();
    try {

      if (getPageBuilder == null) {
        Dao<DbBlock, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

        getPageBuilder = dao.queryBuilder();
        getPageBuilder.where().eq("tag", 1).and().between("height", vPageBegin, vPageEnd);
        getPageBuilder.orderBy("height", true);
      }

      vPageBegin.setValue(begin);
      vPageEnd.setValue(end);

      List<DbBlock> query = getPageBuilder.query();
      for (DbBlock dbBlock : query) {

        Block block = DTOConverter.convert(dbBlock, storage);
        blocks.add(block);
      }
    } catch (SQLException e) {
      throw new DataAccessException();
    }

    blocks.sort(new BlockComparator());
    return blocks;
  }

  public Block getByHeight(int height) {
    Block block = null;

    try {

      if (getByHeightBuilder == null) {

        Dao<DbBlock, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

        getByHeightBuilder = dao.queryBuilder();
        getByHeightBuilder.where().eq("height", vHeight).and().eq("tag", 1);
      }

      vHeight.setValue(height);

      DbBlock dbBlock = getByHeightBuilder.queryForFirst();
      if (dbBlock != null) {
        block = DTOConverter.convert(dbBlock, storage);
      }
    } catch (SQLException e) {
      throw new DataAccessException();
    }

    return block;
  }

  public Block getById(BlockID blockID) {

    Block block = null;

    try {

      if (getByIdBuilder == null) {

        Dao<DbBlock, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

        getByIdBuilder = dao.queryBuilder();
        getByIdBuilder.where().eq("id", vBlockID);
      }
      vBlockID.setValue(blockID.getValue());

      DbBlock dbBlock = getByIdBuilder.queryForFirst();
      if (dbBlock != null && dbBlock.getTag() != 0) {

        block = DTOConverter.convert(dbBlock, storage);
      }
    } catch (SQLException e) {
      throw new DataAccessException(e);
    }

    return block;
  }

  public List<Block> getByAccountId(AccountID id) {
    return getByAccountId(id, 20);
  }

  public List<Block> getByAccountId(AccountID accountID, long limit) {

    List<Block> items = new ArrayList<>();

    try {

      if (getByAccountIdBuilder == null) {

        Dao<DbBlock, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

        getByAccountIdBuilder = dao.queryBuilder();
        getByAccountIdBuilder.where().eq("tag", 1).and().eq("sender_id", vAccountID);
        getByAccountIdBuilder.orderBy("height", false);
      }

      vAccountID.setValue(accountID.getValue());
      getByAccountIdBuilder.limit(limit);

      List<DbBlock> query = getByAccountIdBuilder.query();
      for (DbBlock dbBlock : query) {

        if (dbBlock != null) {

          Block block = DTOConverter.convert(dbBlock, storage);
          items.add(block);
        }
      }
    } catch (SQLException e) {
      throw new DataAccessException();
    }

    return items;
  }

  public Block getLastBlock() {

    try {
      BlockID blockID = storage.metadata().getLastBlockID();
      if (getByIdBuilder == null) {

        Dao<DbBlock, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

        getByIdBuilder = dao.queryBuilder();
        getByIdBuilder.where().eq("id", vBlockID);
      }
      vBlockID.setValue(blockID.getValue());
      DbBlock dbBlock = getByIdBuilder.queryForFirst();
      return DTOConverter.convert(dbBlock, storage);
    } catch (SQLException e) {
      throw new DataAccessException();
    }
  }

  public Block getBlockWithTransaction(TransactionID transactionID) {

    Block block = null;

    try {

      if (getBlockByTxIdBuilder == null) {
        Dao<DbTransaction, Long> txDao =
            DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);
        QueryBuilder<DbTransaction, Long> getTransactionByIdBuilder = txDao.queryBuilder();
        getTransactionByIdBuilder
            .selectColumns("block_id")
            .where()
            .eq("id", transactionID.getValue())
            .and()
            .eq("tag", 1);

        Dao<DbBlock, Long> blockDao =
            DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);
        getBlockByTxIdBuilder = blockDao.queryBuilder();
        getBlockByTxIdBuilder.where().in("id", getTransactionByIdBuilder);
      }

      vTransactionID.setValue(transactionID.getValue());

      DbBlock dbBlock = getBlockByTxIdBuilder.queryForFirst();

      if (dbBlock != null) {

        block = DTOConverter.convert(dbBlock, storage);
      }
    } catch (SQLException e) {
      throw new DataAccessException(e);
    }

    return block;
  }

  private static class BlockComparator implements Comparator<Block> {

    @Override
    public int compare(Block o1, Block o2) {
      return Integer.compare(o2.getHeight(), o1.getHeight());
    }
  }
}
