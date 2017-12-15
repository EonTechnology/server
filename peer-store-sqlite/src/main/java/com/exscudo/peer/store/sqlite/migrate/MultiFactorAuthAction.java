package com.exscudo.peer.store.sqlite.migrate;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.store.sqlite.ConnectionProxy;
import com.exscudo.peer.store.sqlite.utils.SettingName;
import com.exscudo.peer.store.sqlite.utils.TransactionHelper;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class MultiFactorAuthAction {
	private static final String dbScript = "/com/exscudo/peer/store/sqlite/DBv3.sql";

	public static void migrate(ConnectionProxy proxy) throws SQLException, IOException {

		Statement statement = proxy.getConnection().createStatement();
		statement.executeUpdate("begin immediate;");

		int db_version;
		try {
			String r = null;
			ResultSet set = statement.executeQuery("select value from settings where name = '" + SettingName.dbVersion + "';");
			if (set.next()) {
				r = set.getString(1);
				set.close();
			}
			db_version = Integer.parseInt(r, 10);
		} catch (Exception e) {
			throw new UnsupportedOperationException("Illegal database version.");
		}
		if (db_version != 2) {
			throw new IllegalStateException("Unknown version of the database.");
		}

		StatementUtils.runSqlScript(statement, dbScript);
		updateRegTransactions(proxy);
		statement.executeUpdate("commit;");
	}

	/**
	 * Sets the field Recipient of registration transaction
	 * @param proxy
	 */
	private static void updateRegTransactions(ConnectionProxy proxy) throws SQLException {

		ArrayList<Long> ids = new ArrayList<>();
		try(PreparedStatement ps = proxy.prepareStatement("select \"id\" from \"transaction\" where \"type\" = ?1")) {
			ps.setInt(1, TransactionType.AccountRegistration);
			try(ResultSet set = ps.executeQuery()) {
				while (set.next()) {
					ids.add(set.getLong(1));
				}
			}
		}

		for(long id : ids) {
			Transaction tr = TransactionHelper.get(proxy, id);
			TransactionHelper.save(proxy, tr);
		}
	}
}
