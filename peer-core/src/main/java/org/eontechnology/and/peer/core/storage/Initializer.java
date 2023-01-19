package org.eontechnology.and.peer.core.storage;

import com.j256.ormlite.db.SqliteDatabaseType;
import com.j256.ormlite.jdbc.JdbcSingleConnectionSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eontechnology.and.peer.core.IFork;
import org.eontechnology.and.peer.core.common.Loggers;
import org.eontechnology.and.peer.core.storage.migrate.DBInitMigrateAction;
import org.eontechnology.and.peer.core.storage.migrate.DBv2MigrateAction;
import org.eontechnology.and.peer.core.storage.migrate.DBv3MigrateAction;
import org.eontechnology.and.peer.core.storage.migrate.DBv4MigrateAction;
import org.eontechnology.and.peer.core.storage.migrate.DBv5MigrateAction;
import org.eontechnology.and.peer.core.storage.migrate.IMigrate;

/**
 * Basic implementation of the {@code IInitializer} interface.
 *
 * <p>Controls the version of the database and performs the necessary migration actions.
 */
public class Initializer implements IInitializer {
  private final IFork fork;

  public Initializer(IFork fork) {
    this.fork = fork;
  }

  @Override
  public void initialize(Storage storage) throws IOException {
    storage.run(
        new IStorageAction() {
          @Override
          public void run(Connection connection) throws SQLException, IOException {
            initialize(connection);
          }
        });
  }

  private void initialize(Connection connection) throws IOException, SQLException {

    List<IMigrate> migrates = new ArrayList<>();
    migrates.add(new DBInitMigrateAction(connection));
    migrates.add(new DBv2MigrateAction(connection));
    migrates.add(new DBv3MigrateAction(connection, fork));
    migrates.add(new DBv4MigrateAction(connection));
    migrates.add(new DBv5MigrateAction(connection));

    try (JdbcSingleConnectionSource connectionSource =
        new JdbcSingleConnectionSource(
            connection.getMetaData().getURL(), new SqliteDatabaseType(), connection)) {
      connectionSource.initialize();
      Storage.Metadata metadata = new Storage.Metadata(connectionSource);
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

      int lastVersion = migrate(actualMigrates);

      metadata.setVersion(lastVersion);
    }
  }

  private int migrate(List<IMigrate> actualMigrates) throws IOException, SQLException {
    Loggers.info(Initializer.class, "Database migration started");

    // Step 1 - migrating the Database Structure
    for (IMigrate migrate : actualMigrates) {
      Loggers.info(
          Initializer.class,
          "Migrating the Database Structure (up to version " + migrate.getTargetVersion() + ")...");
      migrate.migrateDataBase();
    }

    // Step 2 - data migration
    for (IMigrate migrate : actualMigrates) {
      Loggers.info(
          Initializer.class,
          "Data migration (up to version " + migrate.getTargetVersion() + ")...");
      migrate.migrateLogicalStructure();
    }

    // Step 3 - cleaning after migration
    for (IMigrate migrate : actualMigrates) {
      Loggers.info(
          Initializer.class,
          "Cleaning after migration to version " + migrate.getTargetVersion() + "...");
      migrate.cleanUp();
    }

    Loggers.info(Initializer.class, "Database migration completed");

    IMigrate lastMigration = actualMigrates.get(actualMigrates.size() - 1);
    return lastMigration.getTargetVersion();
  }
}
