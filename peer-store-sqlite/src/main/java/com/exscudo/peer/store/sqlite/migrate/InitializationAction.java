package com.exscudo.peer.store.sqlite.migrate;

import com.exscudo.peer.store.sqlite.ConnectionProxy;
import com.exscudo.peer.store.sqlite.utils.SettingHelper;
import com.exscudo.peer.store.sqlite.utils.SettingName;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializing the structure of the first version DB.
 */
public class InitializationAction {

	private static final String dbScript = "/com/exscudo/peer/store/sqlite/DBv1.sql";
	private static final String initScript = "/com/exscudo/peer/store/sqlite/init.sql";

	public static void migrate(ConnectionProxy connectionProxy) throws SQLException, IOException {

		Statement statement = connectionProxy.getConnection().createStatement();

		int db_version = 0;
		try {
			db_version = Integer.parseInt(SettingHelper.getValue(connectionProxy, SettingName.dbVersion), 10);
		} catch (Exception ignored) {
		}

		if (db_version != 0) {
			throw new IllegalStateException("Unknown version of the database");
		}

		statement.executeUpdate("BEGIN IMMEDIATE;");
		StatementUtils.runSqlScript(statement, dbScript);
		StatementUtils.runSqlScript(statement, initScript);
		statement.executeUpdate("COMMIT;");

	}

}
