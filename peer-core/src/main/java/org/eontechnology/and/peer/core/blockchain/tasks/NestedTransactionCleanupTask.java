package org.eontechnology.and.peer.core.blockchain.tasks;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import java.util.concurrent.Callable;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.IFork;
import org.eontechnology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechnology.and.peer.core.blockchain.storage.DbNestedTransaction;
import org.eontechnology.and.peer.core.common.Loggers;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.storage.Storage;

/** Performs the task of cleaning the Nested Transaction cache. */
public class NestedTransactionCleanupTask implements Runnable {

  private final Storage storage;
  private final IBlockchainProvider blockchainService;
  private final IFork fork;

  // region Prepared Statements

  private DeleteBuilder<DbNestedTransaction, Long> deleteBuilder = null;
  private ArgumentHolder vHeight = new ThreadLocalSelectArg();

  // endregion

  public NestedTransactionCleanupTask(
      IBlockchainProvider blockchainService, Storage storage, IFork fork) {

    this.blockchainService = blockchainService;
    this.storage = storage;
    this.fork = fork;
  }

  @Override
  public void run() {
    try {

      Block lastBlock = blockchainService.getLastBlock();

      int height =
          fork.getTargetBlockHeight(lastBlock.getTimestamp() - 2 * Constant.SECONDS_IN_DAY);
      storage.callInTransaction(
          new Callable<Object>() {

            @Override
            public Object call() throws Exception {

              if (deleteBuilder == null) {
                Dao<DbNestedTransaction, Long> dao =
                    DaoManager.createDao(storage.getConnectionSource(), DbNestedTransaction.class);

                deleteBuilder = dao.deleteBuilder();
                deleteBuilder.where().lt("height", vHeight);
              }

              vHeight.setValue(height);
              deleteBuilder.delete();

              return null;
            }
          });
    } catch (Throwable e) {
      Loggers.error(NestedTransactionCleanupTask.class, "Unable to perform task.", e);
    }
  }
}
