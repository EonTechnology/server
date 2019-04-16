package org.eontechnology.and.peer.core.storage.migrate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializing the structure of the first version DB.
 */
public class DBInitMigrateAction implements IMigrate {

    private final Connection connection;

    public DBInitMigrateAction(Connection connection) {

        this.connection = connection;
    }

    @Override
    public void migrateDataBase() throws IOException, SQLException {
        try (Statement statement = connection.createStatement()) {
            StatementUtils.runSqlScript(statement, "/org/eontechnology/and/peer/store/sqlite/MigrateV1.sql");
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
        return 1;
    }
}
