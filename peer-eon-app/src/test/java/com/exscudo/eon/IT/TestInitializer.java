package com.exscudo.eon.IT;

import com.exscudo.peer.store.sqlite.ConnectionProxy;
import com.exscudo.peer.store.sqlite.Initializer;
import com.exscudo.peer.store.sqlite.migrate.MerkleMigrateAction;
import com.exscudo.peer.store.sqlite.migrate.StatementUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

class TestInitializer extends Initializer {

	@Override
	public void initialize(Connection connection) throws IOException {

		try {

			ConnectionProxy proxy = new ConnectionProxy(connection);

			Statement statement = proxy.getConnection().createStatement();
			statement.executeUpdate("BEGIN IMMEDIATE;");
			StatementUtils.runSqlScript(statement, "/com/exscudo/peer/store/sqlite/DBv1.sql");
			StatementUtils.runSqlScript(statement, "/com/exscudo/eon/IT/init.sql");
			statement.executeUpdate("COMMIT;");

			MerkleMigrateAction.migrate(proxy);

			PreparedStatement stat = connection.prepareStatement("DELETE FROM \"forks\";" );
			stat.execute();
			stat = connection.prepareStatement("INSERT OR REPLACE INTO \"forks\" (\"id\", \"begin\", \"end\", \"target_tx\", \"target_block\") VALUES (1, '2017-10-01T00:00:00.00Z', '2017-11-01T00:00:00.00Z', '1 2', '1');");
			stat.execute();

		} catch (SQLException e) {
			throw new IOException(e);
		}

	}

}
