package com.exscudo.peer.core.storage.migrate;

import java.io.IOException;
import java.sql.SQLException;

public interface IMigrate {

    void migrateDataBase() throws IOException, SQLException;

    void migrateLogicalStructure() throws IOException, SQLException;

    void cleanUp() throws IOException, SQLException;

    int getTargetVersion();
}
