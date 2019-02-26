package org.eontechology.and.peer.core.blockchain.tasks;

import java.util.concurrent.Callable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import org.eontechology.and.peer.core.Constant;
import org.eontechology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechology.and.peer.core.blockchain.storage.DbNestedTransaction;
import org.eontechology.and.peer.core.common.Loggers;
import org.eontechology.and.peer.core.data.Block;
import org.eontechology.and.peer.core.storage.Storage;

/**
 * Performs the task of cleaning the Nested Transaction cache.
 */
public class NestedTransactionCleanupTask implements Runnable {

    private final Storage storage;
    private final IBlockchainProvider blockchainService;

    // region Prepared Statements

    private DeleteBuilder<DbNestedTransaction, Long> deleteBuilder = null;
    private ArgumentHolder vHeight = new ThreadLocalSelectArg();

    // endregion

    public NestedTransactionCleanupTask(IBlockchainProvider blockchainService, Storage storage) {

        this.blockchainService = blockchainService;
        this.storage = storage;
    }

    @Override
    public void run() {
        try {

            Block lastBlock = blockchainService.getLastBlock();

            int height = lastBlock.getHeight() - 2 * Constant.BLOCK_IN_DAY;
            storage.callInTransaction(new Callable<Object>() {

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
