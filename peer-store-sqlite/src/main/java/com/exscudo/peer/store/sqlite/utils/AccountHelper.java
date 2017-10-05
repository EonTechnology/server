package com.exscudo.peer.store.sqlite.utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.exscudo.peer.core.exceptions.DataAccessException;
import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.store.sqlite.Account;
import com.exscudo.peer.store.sqlite.ConnectionProxy;

/**
 * Access to account properties
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class AccountHelper {

	/**
	 * Read account state from DB
	 *
	 * @param db
	 *            data connection
	 * @param id
	 *            account id to read
	 * @return account from DB
	 * @throws DataAccessException
	 *             problems with the DB
	 */
	public static Account getAccount(ConnectionProxy db, long id, int blockHeight) {
		try {

			AccountProperty[] properties = findProperties(db, id, blockHeight);
			if (properties.length == 0) {
				return null;
			}
			return new Account(id, properties);

		} catch (Exception e) {
			throw new DataAccessException(e);
		}
	}

	/**
	 * Read account properties for account
	 *
	 * @param db
	 *            data connection
	 * @param id
	 *            account id
	 * @return properties list
	 * @throws DataAccessException
	 *             problems with the DB
	 */
	private static AccountProperty[] findProperties(ConnectionProxy db, long id, int blockHeight) throws SQLException {

		PreparedStatement statement = db.prepareStatement(
				"select a.\"type\", a.\"account\", a.\"value\", a.\"blockid\", a.\"height\" from \"property\" a inner join( select \"type\", \"account\", max(\"height\") \"height\" from \"property\" where \"account\"=?1 and \"height\" <= ?2 group by \"account\", \"type\") b on a.\"account\"=b.\"account\" and a.\"type\"=b.\"type\" and a.\"height\"=b.\"height\"");

		synchronized (statement) {
			statement.setLong(1, id);
			statement.setInt(2, blockHeight);

			ArrayList<AccountProperty> list = new ArrayList<>();
			ResultSet set = statement.executeQuery();
			while (set.next()) {
				String value = set.getString("value");

				Map<String, Object> map = new HashMap<>();
				if (value != null && value.length() != 0) {
					Bencode bencode = new Bencode();
					map = bencode.decode(value.getBytes(), Type.DICTIONARY);
				}
				AccountProperty si = new AccountProperty(set.getLong("account"), UUID.fromString(set.getString("type")),
						map);
				si.setHeight(set.getInt("height"));
				list.add(si);
			}
			set.close();
			return list.toArray(new AccountProperty[0]);
		}

	}

}
