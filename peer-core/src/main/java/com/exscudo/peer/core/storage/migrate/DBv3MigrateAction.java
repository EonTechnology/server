package com.exscudo.peer.core.storage.migrate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.blockchain.TransactionMapper;
import com.exscudo.peer.core.blockchain.storage.DbBlock;
import com.exscudo.peer.core.blockchain.storage.DbNestedTransaction;
import com.exscudo.peer.core.blockchain.storage.converters.DTOConverter;
import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.storage.Storage;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.db.SqliteDatabaseType;
import com.j256.ormlite.jdbc.JdbcSingleConnectionSource;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;

/**
 * Initializing the structure of the first version DB.
 */
public class DBv3MigrateAction implements IMigrate {

    private final Connection connection;
    private final IFork fork;

    public DBv3MigrateAction(Connection connection, IFork fork) {

        this.connection = connection;
        this.fork = fork;
    }

    @Override
    public void migrateDataBase() throws IOException, SQLException {
        try (Statement statement = connection.createStatement()) {
            StatementUtils.runSqlScript(statement, "/com/exscudo/peer/store/sqlite/MigrateV3_1_structure.sql");
        }
    }

    @Override
    public void migrateLogicalStructure() throws IOException, SQLException {
        try (JdbcSingleConnectionSource connectionSource = new JdbcSingleConnectionSource(connection.getMetaData()
                                                                                                    .getURL(),
                                                                                          new SqliteDatabaseType(),
                                                                                          connection)) {
            connectionSource.initialize();
            migrateLogicalStructure(connectionSource);
        }
    }

    @Override
    public void cleanUp() throws IOException, SQLException {
        try (Statement statement = connection.createStatement()) {
            StatementUtils.runSqlScript(statement, "/com/exscudo/peer/store/sqlite/MigrateV3_3_clean.sql");
        }
    }

    @Override
    public int getTargetVersion() {
        return 3;
    }

    private void migrateLogicalStructure(ConnectionSource connectionSource) throws SQLException, IOException {

        Storage storage = new Storage(null) {
            @Override
            public ConnectionSource getConnectionSource() {
                return connectionSource;
            }

            @Override
            public <TResult> TResult callInTransaction(Callable<TResult> callable) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new DataAccessException(e);
                }
            }
        };

        TransactionMapper transactionMapper = new TransactionMapper(storage, fork);

        Dao<DbNestedTransaction, Long> daoNestedTx = DaoManager.createDao(connectionSource, DbNestedTransaction.class);
        DeleteBuilder<DbNestedTransaction, Long> deleteBuilder = daoNestedTx.deleteBuilder();
        deleteBuilder.delete();

        Dao<DbBlock, Long> daoBlocks = DaoManager.createDao(connectionSource, DbBlock.class);
        QueryBuilder<DbBlock, Long> query = daoBlocks.queryBuilder();
        try (CloseableIterator<DbBlock> i = query.iterator()) {
            while (i.hasNext()) {
                DbBlock dbBlock = i.next();
                if (dbBlock != null) {

                    transactionMapper.map(DTOConverter.convert(dbBlock, storage));
                }
            }
        }
    }
}
