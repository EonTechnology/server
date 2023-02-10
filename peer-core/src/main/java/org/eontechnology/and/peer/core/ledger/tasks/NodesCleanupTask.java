package org.eontechnology.and.peer.core.ledger.tasks;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.common.Loggers;
import org.eontechnology.and.peer.core.ledger.storage.DbNode;
import org.eontechnology.and.peer.core.ledger.tree.TreeNodeID;
import org.eontechnology.and.peer.core.storage.Storage;

/** Performs the task of cleaning StateTree Tree. */
public class NodesCleanupTask implements Runnable {

  private final Storage storage;
  private Dao<DbNode, Long> dao = null;

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
              dao.queryRaw("select `value` from `settings` where `name` = 'LastBlockId'")
                  .getFirstResult()[0]);
      int blockTime =
          Integer.parseInt(
              dao.queryRaw(
                      "select `timestamp` from `blocks` where `id` = ?", Long.toString(lastBlockID))
                  .getFirstResult()[0]);
      int timestamp = blockTime - (2 * Constant.SECONDS_IN_DAY);

      storage.callInTransaction(
          new Callable<Object>() {
            @Override
            public Object call() throws Exception {
              doClean(timestamp);
              return null;
            }
          });

      Loggers.info(
          NodesCleanupTask.class,
          "Nodes that are not used in the states since the '{}' were deleted.",
          new Date(timestamp * 1000L));
    } catch (Throwable e) {
      Loggers.error(NodesCleanupTask.class, "Unable to perform task.", e);
    }
  }

  private void doClean(int timestamp) throws IOException, SQLException {
    Map<Long, List<Long>> links = step1_getLinks();
    step2_removeUsedLinks(timestamp, links);
    step3_dropUnused(links.keySet());
  }

  private Map<Long, List<Long>> step1_getLinks() throws IOException, SQLException {
    Map<Long, List<Long>> links = new LinkedHashMap<>();

    try (CloseableIterator<String[]> nodes =
        dao.queryRaw("select `index`, `right_node_id`, `left_node_id` from `nodes`")
            .closeableIterator()) {

      while (nodes.hasNext()) {
        String[] s = nodes.next();
        long id = Long.parseLong(s[0]);

        if (s[1] != null && s[2] != null) {
          if (links.containsKey(id)) {
            List<Long> items = new ArrayList<>(4);
            items.addAll(links.get(id));
            items.addAll(List.of(new TreeNodeID(s[1]).getIndex(), new TreeNodeID(s[2]).getIndex()));
            links.put(id, items);
          } else {
            links.put(
                id, List.of(new TreeNodeID(s[1]).getIndex(), new TreeNodeID(s[2]).getIndex()));
          }
        } else {
          links.put(id, null);
        }
      }
    }

    return links;
  }

  private void step2_removeUsedLinks(int timestamp, Map<Long, List<Long>> links)
      throws IOException, SQLException {
    try (CloseableIterator<String[]> snapshots =
        dao.queryRaw(
                "select `snapshot` from `blocks` where `timestamp` >= ?",
                Integer.toString(timestamp))
            .closeableIterator()) {

      while (snapshots.hasNext()) {
        String[] s = snapshots.next();
        goDeepAccessibly(new TreeNodeID(s[0]).getIndex(), links);
      }
    }
  }

  protected void step3_dropUnused(Set<Long> links) throws SQLException {

    if (links.size() == 0) {
      return;
    }

    final int LIMIT = 1000;
    StringBuilder sqlBuilder = new StringBuilder("delete from `nodes` where `index` in (?");
    for (int i = 1; i < LIMIT; i++) {
      sqlBuilder.append(",?");
    }
    sqlBuilder.append(")");

    final String sql = sqlBuilder.toString();
    List<String> tmp = new ArrayList<>(LIMIT);

    for (Long index : links) {
      tmp.add(Long.toString(index));
      if (tmp.size() == LIMIT) {
        dao.updateRaw(sql, tmp.toArray(new String[0]));
        tmp.clear();
      }
    }

    String first = Long.toString(links.iterator().next());
    while (tmp.size() < LIMIT) {
      tmp.add(first);
    }

    dao.updateRaw(sql, tmp.toArray(new String[0]));
  }

  private void goDeepAccessibly(Long key, Map<Long, List<Long>> links) {
    List<Long> link = links.get(key);
    if (link != null) {
      for (Long l : link) {
        goDeepAccessibly(l, links);
      }
    }
    links.remove(key);
  }
}
