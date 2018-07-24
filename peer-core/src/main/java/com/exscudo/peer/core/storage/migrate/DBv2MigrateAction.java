package com.exscudo.peer.core.storage.migrate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializing the structure of the first version DB.
 */
public class DBv2MigrateAction implements IMigrate {

    private final Connection connection;

    public DBv2MigrateAction(Connection connection) {

        this.connection = connection;
    }

    @Override
    public void migrateDataBase() throws IOException, SQLException {
        try (Statement statement = connection.createStatement()) {
            StatementUtils.runSqlScript(statement, "/com/exscudo/peer/store/sqlite/MigrateV2.sql");
        }
    }

    @Override
    public void migrateLogicalStructure() {

    }

    @Override
    public void cleanUp() {
    }

    @Override
    public int getTargetVersion() {
        return 2;
    }
}
