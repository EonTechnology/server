package com.exscudo.peer.core.ledger.storage;

import java.sql.SQLException;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import com.j256.ormlite.support.ConnectionSource;

public class DbNodeHelper {

    private final Dao<DbNode, Long> dao;
    private QueryBuilder<DbNode, Long> queryBuilder = null;
    private ArgumentHolder vIndex = new ThreadLocalSelectArg();
    private ArgumentHolder vKey = new ThreadLocalSelectArg();

    public DbNodeHelper(ConnectionSource connectionSource) {
        try {
            this.dao = DaoManager.createDao(connectionSource, DbNode.class);

            queryBuilder = dao.queryBuilder();
            queryBuilder.where().eq("index", vIndex).and().eq("key", vKey);
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
}
