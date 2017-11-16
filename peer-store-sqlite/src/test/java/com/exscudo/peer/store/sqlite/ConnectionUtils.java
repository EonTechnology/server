package com.exscudo.peer.store.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.sqlite.SQLiteConfig;

import com.exscudo.peer.store.sqlite.migrate.StatementUtils;

class ConnectionUtils {

	public static Connection create(String url) throws Exception {
		Class.forName("org.sqlite.JDBC");
		SQLiteConfig config = new SQLiteConfig();
		config.setJournalMode(SQLiteConfig.JournalMode.WAL);
		config.setBusyTimeout("5000");
		config.setTransactionMode(SQLiteConfig.TransactionMode.EXCLUSIVE);

		Connection conection = DriverManager.getConnection("jdbc:sqlite:", config.toProperties());

		Statement statement = conection.createStatement();
		statement.executeUpdate("BEGIN IMMEDIATE;");
		StatementUtils.runSqlScript(statement, "/com/exscudo/peer/store/sqlite/DBv1.sql");
		StatementUtils.runSqlScript(statement, "/com/exscudo/peer/store/sqlite/DBv2.sql");
		StatementUtils.runSqlScript(statement, "/com/exscudo/peer/store/sqlite/DBv2_clean.sql");
		StatementUtils.runSqlScript(statement, url);
		statement.executeUpdate("COMMIT;");

		return conection;
	}

}
