package com.exscudo.peer.core.storage.migrate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DB2MigrateAction implements IMigrate {

    private final Connection connection;

    public DB2MigrateAction(Connection connection) {

        this.connection = connection;
    }

    @Override
    public void migrateDataBase() throws IOException, SQLException {
        try (Statement statement = connection.createStatement()) {
            StatementUtils.runSqlScript(statement, "/com/exscudo/peer/store/sqlite/MigrateV2.sql");
        }
    }

    @Override
    public void migrateLogicalStructure() throws IOException, SQLException {

    }

    @Override
    public void cleanUp() throws IOException, SQLException {

    }

    @Override
    public int getTargetVersion() {
        return 2;
    }
}
