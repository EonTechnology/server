package org.eontechology.and.peer.core.ledger.storage;

import java.sql.SQLException;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.support.ConnectionSource;

//WARNING: Nodes are cached before saving. Data can be corrupted if helper used directly
public class DbNodeHelper {

    private final Dao<DbNode, Long> dao;
    private QueryBuilder<DbNode, Long> queryBuilder = null;
    private ArgumentHolder vIndex = new ThreadLocalSelectArg();
    private ArgumentHolder vKey = new ThreadLocalSelectArg();

    private UpdateBuilder<DbNode, Long> updateBuilder = null;
    private ArgumentHolder vTimestamp = new ThreadLocalSelectArg();

    public DbNodeHelper(ConnectionSource connectionSource) {
        try {
            this.dao = DaoManager.createDao(connectionSource, DbNode.class);

            queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq("index", vIndex).and().eq("key", vKey);

            updateBuilder = dao.updateBuilder().updateColumnValue("timestamp", vTimestamp);
            updateBuilder.where().eq("index", vIndex).and().eq("key", vKey);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void put(DbNode node) throws SQLException {
        dao.create(node);
    }

    public DbNode get(String key, long index) throws SQLException {

        vIndex.setValue(index);
        vKey.setValue(key);
        DbNode dbn = queryBuilder.queryForFirst();

        return dbn;
    }

    public boolean updateTimestamp(String key, long index, int timestamp) throws SQLException {
        vIndex.setValue(index);
        vKey.setValue(key);
        vTimestamp.setValue(timestamp);

        int updated = updateBuilder.update();

        return updated > 0;
    }
}
