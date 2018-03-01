package com.exscudo.peer.core.ledger.tasks;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.storage.DbNode;
import com.exscudo.peer.core.storage.Storage;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;

/**
 * Performs the task of cleaning State Tree.
 */
public class NodesCleanupTask implements Runnable {

    private final Storage storage;
    private final Set<String> colorizedSet = new HashSet<>();

    public NodesCleanupTask(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void run() {

        try {

            discardColoring();
            while (true) {

                BlockID lastBlockID = storage.metadata().getLastBlockID();

                Loggers.trace(NodesCleanupTask.class,
                              "Beginning colorizing. Last block id '{}'",
                              lastBlockID.toString());

                int timestamp = colorizeTree(lastBlockID);

                boolean completed = storage.callInTransaction(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {

                        BlockID newLastBlockID = storage.metadata().getLastBlockID();

                        if (lastBlockID.equals(newLastBlockID)) {
                            removeColorlessNodes(timestamp);
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

                if (completed) {
                    Loggers.info(NodesCleanupTask.class,
                                 "Nodes that are not used in the states since the '{}' were deleted.",
                                 timestamp);
                    break;
                }
            }
        } catch (Throwable e) {
            Loggers.error(NodesCleanupTask.class, "Unable to perform task.", e);
        }
    }

    private void discardColoring() throws SQLException {

        colorizedSet.clear();

        Dao<DbNode, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbNode.class);
        dao.executeRaw("update nodes set color = 0");
    }

    private void removeColorlessNodes(int timestamp) throws SQLException {
        Dao<DbNode, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbNode.class);
        dao.executeRaw("delete from nodes where color = 0 and timestamp <= ?1", String.valueOf(timestamp));
    }

    private int colorizeTree(BlockID lastBlockID) throws SQLException {

        Dao<DbNode, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbNode.class);

        int timestamp = Integer.parseInt(dao.queryRaw("select timestamp from blocks where id = ?1",
                                                      String.valueOf(lastBlockID.getValue())).getFirstResult()[0]);
        timestamp = timestamp - (2 * Constant.SECONDS_IN_DAY);

        final List<String[]> snapshots =
                dao.queryRaw("select distinct snapshot from blocks where timestamp >= ?1", String.valueOf(timestamp))
                   .getResults();
        for (String[] snapshot : snapshots) {

            String key = snapshot[0];
            if (colorizedSet.contains(key)) {
                continue;
            }

            dao.executeRaw("update nodes set color = 1 where key in ( " +
                                   "with recursive tree(id, right_id, left_id) as ( " +
                                   "select key, right_node_id, left_node_id from nodes where key = ?1 " +
                                   "union " +
                                   "select key, right_node_id, left_node_id from nodes join tree on key = right_id or key = left_id" +
                                   ") select id from tree)", key);
            colorizedSet.add(key);
        }

        return timestamp;
    }
}
