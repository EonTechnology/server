package org.eontechnology.and.peer.core.ledger.tasks;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import com.j256.ormlite.stmt.UpdateBuilder;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.common.Loggers;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.ledger.storage.DbNode;
import org.eontechnology.and.peer.core.ledger.tree.TreeNode;
import org.eontechnology.and.peer.core.ledger.tree.TreeNodeID;
import org.eontechnology.and.peer.core.storage.Storage;

/** Performs the task of cleaning StateTree Tree. */
public class NodesCleanupTask implements Runnable {

  private final Storage storage;
  DeleteBuilder<DbNode, Long> deleteBuilder = null;
  private Dao<DbNode, Long> dao = null;
  private UpdateBuilder<DbNode, Long> updateBuilder = null;
  private ArgumentHolder vTimestamp = new ThreadLocalSelectArg();
  private QueryBuilder<DbNode, Long> getBuilder;
  private ArgumentHolder vIndex = new ThreadLocalSelectArg();
  private ArgumentHolder vKey = new ThreadLocalSelectArg();

  public NodesCleanupTask(Storage storage) throws SQLException {
    this.storage = storage;
  }

  @Override
  public void run() {

    try {
      if (dao == null) {
        dao = DaoManager.createDao(storage.getConnectionSource(), DbNode.class);
      }

      long lastBlockID =
          Long.parseLong(
              dao.queryRaw("select value from settings where name = 'LastBlockId'")
                  .getFirstResult()[0]);
      int blockTime =
          Integer.parseInt(
              dao.queryRaw("select timestamp from blocks where id = ?", Long.toString(lastBlockID))
                  .getFirstResult()[0]);
      int timestamp = blockTime - (2 * Constant.SECONDS_IN_DAY);

      Loggers.trace(
          NodesCleanupTask.class,
          "Beginning colorizing. Last block id '{}'",
          new BlockID(lastBlockID));

      step_1_discardColoring(timestamp);
      step_2_colorizeTree_recursive(timestamp);
      // step_3_colorizeTree_sql(timestamp);
      step_4_removeColorlessNodes(timestamp);

      Loggers.info(
          NodesCleanupTask.class,
          "Nodes that are not used in the states since the '{}' were deleted.",
          new Date(timestamp * 1000L));
    } catch (Throwable e) {
      Loggers.error(NodesCleanupTask.class, "Unable to perform task.", e);
    }
  }

  private void step_1_discardColoring(int timestamp) throws SQLException {

    if (updateBuilder == null) {
      updateBuilder = dao.updateBuilder().updateColumnValue("color", 0);
      updateBuilder.where().lt("timestamp", vTimestamp);
    }

    vTimestamp.setValue(timestamp);
    updateBuilder.update();
  }

  private void step_2_colorizeTree_recursive(int timestamp)
      throws SQLException, InterruptedException {

    String[] firstResult =
        dao.queryRaw(
                "select snapshot from blocks where timestamp = ? and tag = 1",
                Integer.toString(timestamp))
            .getFirstResult();
    if (firstResult == null) {
      return;
    }

    String key = firstResult[0];

    LinkedList<Long> nodes = new LinkedList<>();
    goDepth(key, nodes);

    final int LIMIT = 1000;
    StringBuilder sqlBuilder =
        new StringBuilder("UPDATE `nodes` SET `color` = 1 WHERE `index` in (?");
    for (int i = 1; i < LIMIT; i++) {
      sqlBuilder.append(",?");
    }
    sqlBuilder.append(")");

    final String sql = sqlBuilder.toString();

    storage.callInTransaction(
        new Callable<Object>() {
          @Override
          public Object call() throws Exception {
            LinkedList<String> tmp = new LinkedList<>();

            for (Long index : nodes) {
              tmp.add(Long.toString(index));
              if (tmp.size() == LIMIT) {
                dao.updateRaw(sql, tmp.toArray(new String[0]));
                tmp.clear();
              }
            }

            while (tmp.size() < LIMIT) {
              tmp.add("0");
            }

            dao.updateRaw(sql, tmp.toArray(new String[0]));

            return null;
          }
        });
  }

  private void goDepth(String key, LinkedList<Long> nodes) throws SQLException {

    if (getBuilder == null) {
      getBuilder = dao.queryBuilder();
      getBuilder.selectColumns("right_node_id", "left_node_id", "index", "type");
      getBuilder.where().eq("index", vIndex).and().eq("key", vKey);
    }
    TreeNodeID id = new TreeNodeID(key);

    vIndex.setValue(id.getIndex());
    vKey.setValue(id.getKey());
    DbNode dbNode = getBuilder.queryForFirst();
    nodes.add(dbNode.getIndex());

    if (dbNode.getType() == TreeNode.ROOT) {
      goDepth(dbNode.getLeftNode(), nodes);
      goDepth(dbNode.getRightNode(), nodes);
    }
  }

  private void step_3_colorizeTree_sql(int timestamp) throws SQLException, InterruptedException {

    int step = 1;

    int updated =
        dao.updateRaw(
            "update nodes set color = 1 where color = 0 and timestamp > ?",
            Integer.toString(timestamp));

    updated +=
        dao.updateRaw(
            "update nodes set color = 1 where color = 0 and key in (select snapshot from blocks where timestamp = ? and tag = 1)",
            Integer.toString(timestamp));

    while (updated > 0) {
      updated = 0;

      updated +=
          dao.executeRaw(
              "update nodes set color = ? where color = 0 and key in (select left_node_id from nodes where color = ?)",
              Integer.toString(step + 1),
              Integer.toString(step));

      updated +=
          dao.executeRaw(
              "update nodes set color = ? where color = 0 and key in (select right_node_id from nodes where color = ?)",
              Integer.toString(step + 1),
              Integer.toString(step));

      step++;
    }
  }

  private void step_4_removeColorlessNodes(int timestamp) throws SQLException {
    if (deleteBuilder == null) {
      deleteBuilder = dao.deleteBuilder();
      deleteBuilder.where().eq("color", 0).and().lt("timestamp", vTimestamp);
    }

    vTimestamp.setValue(timestamp);
    deleteBuilder.delete();
  }
}
