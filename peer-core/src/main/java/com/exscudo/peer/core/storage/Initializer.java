package com.exscudo.peer.core.storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.exscudo.peer.core.storage.migrate.DBInitMigrateAction;
import com.exscudo.peer.core.storage.migrate.IMigrate;

/**
 * Basic implementation of the {@code IInitializer} interface.
 * <p>
 * Controls the version of the database and performs the necessary migration
 * actions.
 */
public class Initializer implements IInitializer {

    @Override
    public void initialize(Storage storage) throws IOException {
        storage.run(new IStorageAction() {
            @Override
            public void run(Connection connection, Storage.Metadata metadata) throws SQLException, IOException {
                initialize(connection, metadata);
            }
        });
    }

    private void initialize(Connection connection, Storage.Metadata metadata) throws IOException, SQLException {

        List<IMigrate> migrates = new ArrayList<>();
        migrates.add(new DBInitMigrateAction(connection));

        int db_version = metadata.getVersion();

        List<IMigrate> actualMigrates = new ArrayList<>();
        for (IMigrate migrate : migrates) {
            if (migrate.getTargetVersion() > db_version) {
                actualMigrates.add(migrate);
            }
        }

        // Sorting by targetVersion
        actualMigrates.sort(Comparator.comparingInt(IMigrate::getTargetVersion));

        if (actualMigrates.size() == 0) {
            return;
        }

        // Step 1 - migrating the Database Structure
        for (IMigrate migrate : actualMigrates) {
            migrate.migrateDataBase();
        }

        // Step 2 - data migration
        for (IMigrate migrate : actualMigrates) {
            migrate.migrateLogicalStructure();
        }

        // Step 3 - cleaning after migration
        for (IMigrate migrate : actualMigrates) {
            migrate.cleanUp();
        }

        IMigrate lastMigration = actualMigrates.get(actualMigrates.size() - 1);
        int lastVersion = lastMigration.getTargetVersion();

        metadata.setVersion(lastVersion);
    }
}
