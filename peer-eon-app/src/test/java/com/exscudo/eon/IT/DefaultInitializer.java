package com.exscudo.eon.IT;

import com.exscudo.peer.store.sqlite.ConnectionProxy;
import com.exscudo.peer.store.sqlite.IInitializer;
import com.exscudo.peer.store.sqlite.migrate.StatementUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DefaultInitializer implements IInitializer {
	private final String[] initScripts;

	public DefaultInitializer(String[] initScripts) {
		this.initScripts = initScripts;
	}

	@Override
	public void initialize(Connection connection) throws IOException {
		try {

			ConnectionProxy proxy = new ConnectionProxy(connection);
			Statement statement = proxy.getConnection().createStatement();
			statement.executeUpdate("BEGIN IMMEDIATE;");
			StatementUtils.runSqlScript(statement, "/com/exscudo/peer/store/sqlite/DBv1.sql");
			StatementUtils.runSqlScript(statement, "/com/exscudo/peer/store/sqlite/DBv2.sql");
			StatementUtils.runSqlScript(statement, "/com/exscudo/peer/store/sqlite/DBv2_clean.sql");
			StatementUtils.runSqlScript(statement, "/com/exscudo/peer/store/sqlite/DBv3.sql");
			for (String script : initScripts) {
				StatementUtils.runSqlScript(statement, script);
			}
			statement.executeUpdate("COMMIT;");

		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
}
