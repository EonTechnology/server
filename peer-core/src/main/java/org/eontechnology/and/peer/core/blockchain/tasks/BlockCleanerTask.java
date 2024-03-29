package org.eontechnology.and.peer.core.blockchain.tasks;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechnology.and.peer.core.blockchain.storage.DbAccTransaction;
import org.eontechnology.and.peer.core.blockchain.storage.DbBlock;
import org.eontechnology.and.peer.core.blockchain.storage.DbTransaction;
import org.eontechnology.and.peer.core.common.Loggers;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.storage.Storage;

/** Performs the task of removing side chains. */
public class BlockCleanerTask implements Runnable {

  private final Storage storage;
  private final IBlockchainProvider blockchainService;

  // region Prepared Statements

  private QueryBuilder<DbBlock, Long> blocksQueryBuilder = null;
  private DeleteBuilder<DbBlock, Long> blocksDeleteBuilder = null;
  private DeleteBuilder<DbTransaction, Long> transactionsDeleteBuilder = null;
  private DeleteBuilder<DbAccTransaction, Long> accTransactionsDeleteBuilder = null;

  private ArgumentHolder vTimestamp = new ThreadLocalSelectArg();
  private ArgumentHolder vID = new ThreadLocalSelectArg();
  private ArgumentHolder vBlockID = new ThreadLocalSelectArg();
  private ArgumentHolder vAccBlockID = new ThreadLocalSelectArg();
  private ArgumentHolder vTag = new ThreadLocalSelectArg();

  // endregion

  public BlockCleanerTask(IBlockchainProvider blockchainService, Storage storage) {

    this.blockchainService = blockchainService;
    this.storage = storage;
  }

  @Override
  public void run() {

    try {
      cleanTransactionAndBlock();
    } catch (Throwable e) {
      Loggers.error(BlockCleanerTask.class, "Unable to perform task.", e);
    }
  }

  private void cleanTransactionAndBlock() throws SQLException {

    List<BlockID> blocksIds = new LinkedList<>();

    blocksIds.addAll(getForkedBlocksIds());
    blocksIds.addAll(getOldHistoryBlocksIds());

    storage.callInTransaction(
        new Callable<Object>() {

          @Override
          public Object call() throws Exception {
            for (BlockID id : blocksIds) {
              removeBlock(id.getValue());
              removeAccTransactions(id.getValue());
              removeTransactions(id.getValue());
            }
            return null;
          }
        });
  }

  private List<BlockID> getForkedBlocksIds() throws SQLException {

    initQueryBuilder();

    Block lastBlock = blockchainService.getLastBlock();
    int timestamp = lastBlock.getTimestamp() - Constant.SECONDS_IN_DAY;

    vTimestamp.setValue(timestamp);
    vTag.setValue(0);

    return getBlockIds(blocksQueryBuilder);
  }

  private List<BlockID> getBlockIds(QueryBuilder<DbBlock, Long> blocksQueryBuilder)
      throws SQLException {
    return new ArrayList<>(getBlockIdsWithHeight(blocksQueryBuilder).keySet());
  }

  private Map<BlockID, Integer> getBlockIdsWithHeight(
      QueryBuilder<DbBlock, Long> blocksQueryBuilder) throws SQLException {
    List<DbBlock> dbBlocks = blocksQueryBuilder.query();
    final Map<BlockID, Integer> res = new HashMap<>();
    for (DbBlock block : dbBlocks) {
      res.put(new BlockID(block.getId()), block.getHeight());
    }

    return res;
  }

  private List<BlockID> getOldHistoryBlocksIds() throws SQLException {

    if (storage.metadata().getProperty("FULL").equals("1")) {
      return new LinkedList<>();
    }

    Block genesis = blockchainService.getBlockByHeight(0);
    Block lastBlock = blockchainService.getLastBlock();
    if (lastBlock.getTimestamp() <= genesis.getTimestamp() + Constant.STORAGE_FRAME_AGE) {
      return new LinkedList<>();
    }

    initQueryBuilder();

    int timestamp = lastBlock.getTimestamp() - Constant.STORAGE_FRAME_AGE;

    vTimestamp.setValue(timestamp);
    vTag.setValue(1);

    int minH = lastBlock.getHeight();
    Map<BlockID, Integer> blockIdsWithHeight = getBlockIdsWithHeight(blocksQueryBuilder);
    for (Integer value : blockIdsWithHeight.values()) {
      minH = Math.min(minH, value);
    }

    storage.metadata().setHistoryFromHeight(minH);

    return new ArrayList<>(blockIdsWithHeight.keySet());
  }

  private void initQueryBuilder() throws SQLException {
    if (blocksQueryBuilder == null) {

      Dao<DbBlock, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

      blocksQueryBuilder = dao.queryBuilder();
      blocksQueryBuilder.selectColumns("id", "height");
      blocksQueryBuilder
          .where()
          .lt("timestamp", vTimestamp)
          .and()
          .eq("tag", vTag)
          .and()
          .gt("height", 0);
    }
  }

  private void removeAccTransactions(long blockID) throws SQLException {

    if (accTransactionsDeleteBuilder == null) {

      Dao<DbAccTransaction, Long> dao =
          DaoManager.createDao(storage.getConnectionSource(), DbAccTransaction.class);

      accTransactionsDeleteBuilder = dao.deleteBuilder();
      accTransactionsDeleteBuilder.where().eq("block_id", vAccBlockID);
    }

    vAccBlockID.setValue(blockID);
    accTransactionsDeleteBuilder.delete();
  }

  private void removeTransactions(long blockID) throws SQLException {

    if (transactionsDeleteBuilder == null) {

      Dao<DbTransaction, Long> dao =
          DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);

      transactionsDeleteBuilder = dao.deleteBuilder();
      transactionsDeleteBuilder.where().eq("block_id", vBlockID);
    }

    vBlockID.setValue(blockID);
    transactionsDeleteBuilder.delete();
  }

  private void removeBlock(long id) throws SQLException {

    if (blocksDeleteBuilder == null) {
      Dao<DbBlock, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);

      blocksDeleteBuilder = dao.deleteBuilder();
      blocksDeleteBuilder.where().eq("id", vID);
    }

    vID.setValue(id);
    blocksDeleteBuilder.delete();
  }
}
