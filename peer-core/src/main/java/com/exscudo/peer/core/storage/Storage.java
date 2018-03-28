package com.exscudo.peer.core.storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.Callable;

import com.exscudo.peer.core.IInitializer;
import com.exscudo.peer.core.InitializerJson;
import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.storage.migrate.StatementUtils;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.db.SqliteDatabaseType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.jdbc.JdbcSingleConnectionSource;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.commons.dbcp2.BasicDataSource;
import org.sqlite.SQLiteConfig;

/**
 * Init DB connection.
 * <p>
 * To create instance use {@link Storage#create}
 */
public class Storage {

    private final BasicDataSource dataSource;
    private ConnectionSource connectionSource;

    public Storage(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    //
    // Static members
    //
    public static Storage create(String connectURI,
                                 String genesisFile,
                                 boolean fullSync) throws IOException, ClassNotFoundException, SQLException {
        return create(connectURI, new InitializerJson(genesisFile, fullSync));
    }

    public static Storage create(String connectURI,
                                 IInitializer loader) throws ClassNotFoundException, IOException, SQLException {

        SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setBusyTimeout("60000");
        config.setTransactionMode(SQLiteConfig.TransactionMode.EXCLUSIVE);
        BasicDataSource dataSource = createDataSource(connectURI, config);

        Storage storage = new Storage(dataSource);
        loader.initialize(storage);
        return storage;
    }

    public static BasicDataSource createDataSource(String connectURI, SQLiteConfig config) {

        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setUrl(connectURI);
        dataSource.setPoolPreparedStatements(true);

        dataSource.setDriverClassName("org.sqlite.JDBC");
        for (Map.Entry<?, ?> e : config.toProperties().entrySet()) {
            dataSource.addConnectionProperty((String) e.getKey(), (String) e.getValue());
        }

        return dataSource;
    }

    public void destroy() {

        // connectionSource.close() - empty for DataSourceConnectionSource
        try {
            dataSource.close();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public ConnectionSource getConnectionSource() {

        if (connectionSource == null) {

            try {
                DataSourceConnectionSource source = new DataSourceConnectionSource(dataSource, dataSource.getUrl());
                this.connectionSource = source;
            } catch (SQLException e) {
                throw new DataAccessException(e);
            }
        }
        return connectionSource;
    }

    public <TResult> TResult callInTransaction(Callable<TResult> callable) {

        try {
            return TransactionManager.callInTransaction(getConnectionSource(), callable);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    /**
     * Update indexes
     */
    public void analyze() {

        run(new IStorageAction() {

            @Override
            public void run(Connection connection, Metadata metadata) throws SQLException, IOException {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("PRAGMA optimize;");
                }
            }
        });
    }

    public void run(String[] scripts) {

        run(new IStorageAction() {

            @Override
            public void run(Connection connection, Metadata metadata) throws SQLException, IOException {
                try (Statement statement = connection.createStatement()) {
                    for (String script : scripts) {
                        StatementUtils.runSqlScript(statement, script);
                    }
                }
            }
        });
    }

    public void run(IStorageAction action) {

        try (Connection connection = dataSource.getConnection()) {

            connection.setAutoCommit(false);
            try {

                try (JdbcSingleConnectionSource connectionSource = new JdbcSingleConnectionSource(connection.getMetaData()
                                                                                                            .getURL(),
                                                                                                  new SqliteDatabaseType(),
                                                                                                  connection)) {
                    connectionSource.initialize();
                    final Metadata metadata = new Metadata(connectionSource);
                    action.run(connection, metadata);
                }
                connection.commit();
            } catch (Throwable t) {
                try {
                    connection.rollback();
                } catch (Exception ignore) {

                }
                throw t;
            }
        } catch (SQLException | IOException e) {
            throw new DataAccessException(e);
        }
    }

    public Metadata metadata() {
        try {
            return new Metadata(getConnectionSource());
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public static class Metadata {

        private final Dao<DatabaseProperty, Long> daoSettings;
        private int historyFromHeight = -1;

        public Metadata(ConnectionSource connectionSource) throws SQLException {
            daoSettings = DaoManager.createDao(connectionSource, DatabaseProperty.class);
        }

        public void setProperty(String name, String value) throws SQLException {
            daoSettings.createOrUpdate(new DatabaseProperty(name, value));
        }

        public String getProperty(String name) throws SQLException {

            QueryBuilder<DatabaseProperty, Long> queryBuilder = daoSettings.queryBuilder();
            queryBuilder.selectColumns("value");
            queryBuilder.where().eq("name", new ThreadLocalSelectArg(SqlType.STRING, name));

            DatabaseProperty first = queryBuilder.queryForFirst();
            if (first == null) {
                return null;
            }
            return first.getValue();
        }

        /**
         * Returns DB version
         *
         * @return
         */
        public int getVersion() {
            try {
                return Integer.parseInt(getProperty("DB_VERSION"), 10);
            } catch (Exception ignore) {
                return 0;
                // throw new DataAccessException(e);
            }
        }

        public void setVersion(int version) {
            try {
                setProperty("DB_VERSION", Integer.toString(version));
            } catch (Exception e) {
                throw new DataAccessException(e);
            }
        }

        /**
         * Returns genesis block ID
         *
         * @return
         */
        public BlockID getGenesisBlockID() {
            try {
                return new BlockID(Long.parseLong(getProperty("GENESIS_BLOCK_ID"), 10));
            } catch (Exception e) {
                throw new DataAccessException(e);
            }
        }

        /**
         * Returns Top block in blockchain
         *
         * @return
         */
        public BlockID getLastBlockID() {
            try {
                return new BlockID(Long.parseLong(getProperty("LastBlockId"), 10));
            } catch (Exception e) {
                throw new DataAccessException(e);
            }
        }

        public void setLastBlockID(BlockID lastBlockID) {
            try {
                setProperty("LastBlockId", Long.toString(lastBlockID.getValue()));
            } catch (Exception e) {
                throw new DataAccessException(e);
            }
        }

        public int getHistoryFromHeight() {
            if (historyFromHeight < 0) {
                try {
                    String id = getProperty("HistoryFromHeight");
                    if (id != null) {
                        historyFromHeight = Integer.parseInt(id);
                    }
                } catch (Exception e) {
                    throw new DataAccessException(e);
                }
            }
            return historyFromHeight;
        }

        public void setHistoryFromHeight(int height) {
            try {
                historyFromHeight = height;
                setProperty("HistoryFromHeight", Integer.toString(height));
            } catch (Exception e) {
                throw new DataAccessException(e);
            }
        }

        @DatabaseTable(tableName = "settings")
        private static class DatabaseProperty {

            @DatabaseField(id = true, columnName = "name", canBeNull = false)
            private String name;

            @DatabaseField(columnName = "value")
            private String value;

            public DatabaseProperty() {

            }

            public DatabaseProperty(String name, String value) {
                this.setName(name);
                this.setValue(value);
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        }
    }
}
