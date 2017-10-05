package com.exscudo.peer.store.sqlite.utils;

import java.sql.Statement;

import com.exscudo.peer.core.exceptions.DataAccessException;
import com.exscudo.peer.store.sqlite.ConnectionProxy;

/**
 * Transaction management on DB (SQLite)
 */
public class DatabaseHelper {

	/**
	 * Commit transaction
	 * 
	 * @param connection
	 *            data connection
	 * @param name
	 *            transaction name
	 */
	public static void commitTransaction(ConnectionProxy connection, String name) {

		try {

			String sql = String.format("RELEASE %s;", name);
			Statement statement = connection.getConnection().createStatement();
			statement.executeUpdate(sql);
			statement.close();

		} catch (Exception e) {
			throw new DataAccessException(e);
		}

	}

	/**
	 * Rollback transaction
	 *
	 * @param connection
	 *            data connection
	 * @param name
	 *            transaction name
	 */
	public static void rollbackTransaction(ConnectionProxy connection, String name) throws DataAccessException {

		try {

			String sql = String.format("ROLLBACK TO %s;", name);
			Statement statement = connection.getConnection().createStatement();
			statement.executeUpdate(sql);
			statement.close();

		} catch (Exception e) {
			throw new DataAccessException(e);
		}

	}

	/**
	 * Begin transaction
	 *
	 * @param connection
	 *            data connection
	 * @param name
	 *            transaction name
	 */
	public static void beginTransaction(ConnectionProxy connection, String name) throws DataAccessException {

		try {

			String sql = String.format("SAVEPOINT %s;", name);
			Statement statement = connection.getConnection().createStatement();
			statement.executeUpdate(sql);
			statement.close();

		} catch (Exception e) {
			throw new DataAccessException(e);
		}

	}

	/**
	 * Update indexes
	 *
	 * @param connection
	 *            data connection
	 */
	public static void analyze(ConnectionProxy connection) throws DataAccessException {

		try {

			Statement statement = connection.getConnection().createStatement();
			statement.executeUpdate("ANALYZE;");
			statement.close();

		} catch (Exception e) {
			throw new DataAccessException(e);
		}

	}

}
