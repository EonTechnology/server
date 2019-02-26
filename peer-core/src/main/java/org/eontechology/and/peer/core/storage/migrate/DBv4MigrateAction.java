package org.eontechology.and.peer.core.storage.migrate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializing the structure of the first version DB.
 */
public class DBv4MigrateAction implements IMigrate {

    private final Connection connection;

    public DBv4MigrateAction(Connection connection) {

        this.connection = connection;
    }

    @Override
    public void migrateDataBase() {
    }

    @Override
    public void migrateLogicalStructure() throws IOException, SQLException {
        try (Statement statement = connection.createStatement()) {
            StatementUtils.runSqlScript(statement, "/org/eontechology/and/peer/store/sqlite/MigrateV4.sql");
        }
    }

    @Override
    public void cleanUp() {
    }

    @Override
    public int getTargetVersion() {
        return 4;
    }
}
