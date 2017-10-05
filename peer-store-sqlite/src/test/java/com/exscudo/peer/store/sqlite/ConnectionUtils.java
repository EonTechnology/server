package com.exscudo.peer.store.sqlite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.sqlite.SQLiteConfig;

class ConnectionUtils {

	public static ConnectionProxy create(String url) throws Exception {
		Class.forName("org.sqlite.JDBC");
		SQLiteConfig config = new SQLiteConfig();
		config.setJournalMode(SQLiteConfig.JournalMode.WAL);
		config.setBusyTimeout("5000");
		config.setTransactionMode(SQLiteConfig.TransactionMode.EXCLUSIVE);

		ConnectionProxy conn = new ConnectionProxy(DriverManager.getConnection("jdbc:sqlite:", config.toProperties()));

		Statement statement = conn.getConnection().createStatement();
		statement.executeUpdate("BEGIN IMMEDIATE;");
		runSqlScript(conn.getConnection(), statement, "/com/exscudo/eon/sqlite/DBv1.sql");
		runSqlScript(conn.getConnection(), statement, url);
		statement.executeUpdate("COMMIT;");

		return conn;
	}

	private static void runSqlScript(Connection connection, Statement statement, String fileName)
			throws IOException, SQLException {

		InputStream inputStream = connection.getClass().getResourceAsStream(fileName);

		BufferedReader r = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		String str = null;
		StringBuilder sb = new StringBuilder(8192);

		while ((str = r.readLine()) != null) {
			sb.append(str);
			sb.append("\n");
		}

		statement.executeUpdate(sb.toString());

	}

}
