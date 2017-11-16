package com.exscudo.peer.store.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.eon.CachedHashMap;
import com.exscudo.peer.store.sqlite.merkle.TreeNode;

/**
 * Caching {@code PreparedStatement}.
 * <p>
 * Uses SQL request as cache key.
 *
 * @see PreparedStatement
 * @see Connection
 */
public class ConnectionProxy {

	private Connection connection;
	private Map<String, PreparedStatement> cachedStatements = new HashMap<>();

	public ConnectionProxy(Connection connection) {
		this.connection = connection;
	}

	public Connection getConnection() {
		return connection;
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {

		if (connection.isClosed()) {
			return null;
		}

		PreparedStatement statement = cachedStatements.get(sql);
		if (statement == null) {
			statement = connection.prepareStatement(sql);
			cachedStatements.put(sql, statement);
		}

		return statement;
	}

	public Map<String, TreeNode> getTreeNodeCache() {
		return cache;
	}
	private Map<String, TreeNode> cache = Collections.synchronizedMap(new CachedHashMap<String, TreeNode>(100000));

}
