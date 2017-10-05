package com.exscudo.peer.store.sqlite.utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.exscudo.peer.core.exceptions.DataAccessException;
import com.exscudo.peer.store.sqlite.ConnectionProxy;

/**
 * Settings management in DB
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class SettingHelper {

	/**
	 * Read internal setting from DB
	 * 
	 * @param db
	 *            data connection
	 * @param name
	 *            setting name
	 * @return setting value
	 * @throws DataAccessException
	 *             problems with the DB
	 */
	public static String getValue(final ConnectionProxy db, final String name) throws DataAccessException {

		try {

			PreparedStatement getStatement = db.prepareStatement("select value from settings where name = ?");
			synchronized (getStatement) {

				getStatement.setString(1, name);
				ResultSet set = getStatement.executeQuery();

				if (set.next()) {

					String result = set.getString(1);
					set.close();

					return result;
				}
			}

			return null;

		} catch (Exception e) {
			throw new DataAccessException(e);
		}

	}

	/**
	 * Write internal setting to DB
	 *
	 * @param db
	 *            data connection
	 * @param name
	 *            setting name
	 * @param value
	 *            setting value
	 * @throws DataAccessException
	 *             problems with the DB
	 */
	public static void setValue(final ConnectionProxy db, final String name, final String value)
			throws DataAccessException {

		try {

			PreparedStatement setStatement = db
					.prepareStatement("insert or replace into settings (name, value) values (?, ?)");
			synchronized (setStatement) {

				setStatement.setString(1, name);
				setStatement.setString(2, value);
				setStatement.executeUpdate();
			}

		} catch (Exception e) {
			throw new DataAccessException(e);
		}

	}
}
