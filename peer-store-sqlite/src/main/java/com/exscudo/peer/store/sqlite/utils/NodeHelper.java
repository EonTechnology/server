package com.exscudo.peer.store.sqlite.utils;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.exscudo.peer.core.exceptions.DataAccessException;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.store.sqlite.ConnectionProxy;
import com.exscudo.peer.store.sqlite.merkle.TreeNode;

/**
 * Access to account properties
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class NodeHelper {

	public static void put(ConnectionProxy db, String key, TreeNode node) {

		try {

			if (get(db, key) != null) {
				return;
			}

			Bencode bencode = new Bencode();
			Map<String, Object> map = node.getValues();
			String value = null;
			if (map != null) {
				value = Format.convert(bencode.encode(map));
			}

			PreparedStatement saveStatement = db.prepareStatement(
					"INSERT OR IGNORE INTO \"nodes\" (\"key\", \"type\", \"id\", \"index\", \"value\")\n VALUES (?, ?, ?, ?, ?);");
			synchronized (saveStatement) {

				saveStatement.setString(1, key);
				saveStatement.setInt(2, node.getType());
				saveStatement.setLong(3, node.getId());
				saveStatement.setLong(4, getIndex(key));
				saveStatement.setString(5, value);
				saveStatement.executeUpdate();

				Map<String, TreeNode> cache = db.getTreeNodeCache();
				cache.put(key, node);

			}

		} catch (Exception e) {
			throw new DataAccessException(e);
		}
	}

	public static TreeNode get(ConnectionProxy db, String key) {
		try {
			Map<String, TreeNode> cache = db.getTreeNodeCache();
			TreeNode node = cache.get(key);
			if (node != null) {
				return node;
			}

			PreparedStatement getStatement = db.prepareStatement(
					"select \"type\", \"id\", \"value\" from nodes where \"index\" = ? and \"key\" = ?");
			synchronized (getStatement) {

				long index = getIndex(key);
				getStatement.setLong(1, index);
				getStatement.setString(2, key);
				ResultSet set = getStatement.executeQuery();

				if (set.next()) {

					int type = set.getInt("type");
					long id = set.getLong("id");
					byte[] value = Format.convert(set.getString("value"));

					Map<String, Object> map = new HashMap<>();
					if (value != null && value.length != 0) {
						Bencode bencode = new Bencode();
						map = bencode.decode(value, Type.DICTIONARY);
					}
					node = new TreeNode(type, id, map);

					set.close();

					cache.put(key, node);
					return node;
				}
			}

			return null;

		} catch (Exception e) {
			throw new DataAccessException(e);
		}
	}

	public static boolean contains(ConnectionProxy db, String key) {
		try {

			Map<String, TreeNode> cache = db.getTreeNodeCache();
			if (cache.containsKey(key)) {
				return true;
			}

			PreparedStatement containStatement = db
					.prepareStatement("select count(1) from \"nodes\" where \"index\" = ? and \"key\" = ?");
			synchronized (containStatement) {

				long index = getIndex(key);
				containStatement.setLong(1, index);
				containStatement.setString(2, key);
				ResultSet set = containStatement.executeQuery();

				boolean result = false;

				if (set.next()) {
					result = set.getInt(1) != 0;
				}

				set.close();
				return result;
			}

		} catch (Exception e) {

			throw new DataAccessException(e);
		}
	}

	public static void remove(ConnectionProxy db, String key) {

		try {

			PreparedStatement removeStatement = db
					.prepareStatement("delete from \"nodes\" where where \"index\" = ? and \"key\" = ?");
			synchronized (removeStatement) {

				long index = getIndex(key);
				removeStatement.setLong(1, index);
				removeStatement.setString(2, key);
				removeStatement.executeUpdate();

				Map<String, TreeNode> cache = db.getTreeNodeCache();
				cache.remove(key);

			}

		} catch (Exception e) {
			throw new DataAccessException(e);
		}

	}

	private static long getIndex(String key) {
		byte[] bytes = key.getBytes();

		if (bytes.length % 8 != 0) {
			bytes = Arrays.copyOf(bytes, ((bytes.length % 8) + 1) * 4);
		}

		BigInteger bigInteger = BigInteger.ZERO;
		for (int i = 0; i < bytes.length; i += 8) {
			BigInteger bi = new BigInteger(1, new byte[]{bytes[i + 7], bytes[i + 6], bytes[i + 5], bytes[i + 4],
					bytes[i + 3], bytes[i + 2], bytes[i + 1], bytes[i]});
			bigInteger = bigInteger.xor(bi);
		}

		return bigInteger.longValue();
	}

}
