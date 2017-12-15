package com.exscudo.peer.store.sqlite;

import com.exscudo.peer.store.sqlite.migrate.InitializationAction;
import com.exscudo.peer.store.sqlite.migrate.MerkleMigrateAction;
import com.exscudo.peer.store.sqlite.migrate.MultiFactorAuthAction;
import com.exscudo.peer.store.sqlite.utils.SettingHelper;
import com.exscudo.peer.store.sqlite.utils.SettingName;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Basic implementation of the {@code IInitializer} interface.
 * <p>
 * Controls the version of the database and performs the necessary migration
 * actions.
 */
public class Initializer implements IInitializer {

	@Override
	public void initialize(Connection connection) throws IOException {

		try {

			ConnectionProxy proxy = new ConnectionProxy(connection);

			int db_version = 0;
			try {
				db_version = Integer.parseInt(SettingHelper.getValue(proxy, SettingName.dbVersion), 10);
			} catch (Exception ignored) {
			}

			switch (db_version) {
				case 0:
					InitializationAction.migrate(proxy);
				case 1:
					MerkleMigrateAction.migrate(proxy);
				case 2:
					MultiFactorAuthAction.migrate(proxy);
				default:
					break;
			}

		} catch (SQLException e) {
			throw new IOException(e);
		}

	}

}
