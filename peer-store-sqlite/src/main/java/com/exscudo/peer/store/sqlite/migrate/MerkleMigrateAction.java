package com.exscudo.peer.store.sqlite.migrate;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.exceptions.DataAccessException;
import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.core.utils.Loggers;
import com.exscudo.peer.eon.Account;
import com.exscudo.peer.eon.transactions.utils.AccountAttributes;
import com.exscudo.peer.eon.transactions.utils.AccountBalance;
import com.exscudo.peer.eon.transactions.utils.AccountDeposit;
import com.exscudo.peer.store.sqlite.ConnectionProxy;
import com.exscudo.peer.store.sqlite.merkle.Ledgers;
import com.exscudo.peer.store.sqlite.utils.BlockHelper;
import com.exscudo.peer.store.sqlite.utils.DatabaseHelper;
import com.exscudo.peer.store.sqlite.utils.SettingHelper;
import com.exscudo.peer.store.sqlite.utils.SettingName;

/**
 * Defines actions performed during database update to the second version.
 *
 */
public class MerkleMigrateAction {
	private static final String updateScript = "/com/exscudo/peer/store/sqlite/DBv2.sql";
	private static final String cleanScript = "/com/exscudo/peer/store/sqlite/DBv2_clean.sql";

	public static void migrate(ConnectionProxy connectionProxy) throws SQLException, IOException {

		updateTables(connectionProxy);
		DatabaseHelper.beginTransaction(connectionProxy, "SnapshotManager");
		try {
			createSnapshots(connectionProxy);
			DatabaseHelper.commitTransaction(connectionProxy, "SnapshotManager");
		} catch (IOException e) {
			DatabaseHelper.rollbackTransaction(connectionProxy, "SnapshotManager");
			throw e;
		}
		clean(connectionProxy);

	}

	private static void updateTables(ConnectionProxy connectionProxy) throws SQLException, IOException {

		Statement statement = connectionProxy.getConnection().createStatement();
		statement.executeUpdate("begin immediate;");

		int db_version = 0;
		try {
			db_version = Integer.parseInt(getValue(statement, SettingName.dbVersion), 10);
		} catch (Exception e) {
			throw new UnsupportedOperationException("Illegal database version.");
		}
		if (db_version != 1) {
			throw new IllegalStateException("Unknown version of the database.");
		}

		if (getValue(statement, SettingName.dbUpdateStep) == null) {
			StatementUtils.runSqlScript(statement, updateScript);
		}

		statement.executeUpdate("commit;");

	}

	private static void clean(ConnectionProxy connectionProxy) throws SQLException, IOException {

		Statement statement = connectionProxy.getConnection().createStatement();
		statement.executeUpdate("begin immediate;");
		StatementUtils.runSqlScript(statement, cleanScript);
		statement.executeUpdate("commit;");

	}

	private static String getValue(Statement statement, String name) throws SQLException {

		String r = null;
		ResultSet set = statement.executeQuery("select value from settings where name = '" + name + "';");
		if (set.next()) {
			r = set.getString(1);
			set.close();
		}
		return r;

	}

	private static void createSnapshots(ConnectionProxy connection) throws IOException {

		try {

			long lastBlockID = Long.parseLong(SettingHelper.getValue(connection, SettingName.lastBlockID), 10);
			Block lastBlock = BlockHelper.get(connection, lastBlockID);
			if (lastBlock == null) {
				throw new IllegalStateException("Data corrupted. Can not find last block.");
			}

			long[] ids = BlockHelper.getBlockLinkedList(connection,
					lastBlock.getHeight() - Constant.SYNC_MILESTONE_DEPTH + 1, lastBlock.getHeight());
			byte[] snapshot = null;
			for (long id : ids) {

				if (id == 0) {
					continue;
				}

				Loggers.info(MerkleMigrateAction.class, "Creating snapshot for block {}", Format.ID.blockId(id));

				Block currBlock = BlockHelper.get(connection, id);
				if (currBlock == null) {
					throw new IllegalStateException("Data corrupted. Unable to get block.");
				}

				if (snapshot == null) {
					snapshot = createSnapshot(connection, currBlock.getHeight());
				} else {
					snapshot = createSnapshot(connection, currBlock, snapshot);
				}
				currBlock.setSnapshot(snapshot);
				BlockHelper.save(connection, currBlock);

			}

		} catch (Exception e) {
			throw new IOException(e);
		}

	}

	private static byte[] createSnapshot(ConnectionProxy connection, int height) throws SQLException {

		ILedger state = Ledgers.newLeger(connection, null);

		List<Long> accounts = getAccounts(connection, height);

		Loggers.info(MerkleMigrateAction.class, "Number of accounts: {}", accounts.size());

		for (Long accountID : accounts) {
			Account account = getAccount(connection, accountID, height);
			if (!validateAccount(account)) {
				throw new AssertionError("Data is corrupted.");
			}
			state.putAccount(account);
		}

		for (Long accountID : accounts) {
			if (state.getAccount(accountID) == null) {
				throw new AssertionError();
			}
		}

		return state.getHash();

	}

	private static List<Long> getAccounts(ConnectionProxy connection, int blockHeight) throws SQLException {

		ArrayList<Long> accounts = new ArrayList<>();
		try {

			PreparedStatement statement = connection.prepareStatement(
					"select \"account\" from \"property\" where \"height\" <= ?1 group by \"account\" ");
			synchronized (statement) {
				statement.setInt(1, blockHeight);

				ResultSet set = statement.executeQuery();
				while (set.next()) {
					long accountID = set.getLong("account");
					accounts.add(accountID);
				}
				set.close();
			}

		} catch (Exception e) {
			throw new DataAccessException(e);
		}

		return accounts;

	}

	private static Account getAccount(ConnectionProxy db, long id, int blockHeight) throws SQLException {

		PreparedStatement statement = db.prepareStatement(
				"select a.\"type\", a.\"account\", a.\"value\", a.\"blockid\", a.\"height\" from \"property\" a inner join( select \"type\", \"account\", max(\"height\") \"height\" from \"property\" where \"account\"=?1 and \"height\" <= ?2 group by \"account\", \"type\") b on a.\"account\"=b.\"account\" and a.\"type\"=b.\"type\" and a.\"height\"=b.\"height\"");

		synchronized (statement) {
			statement.setLong(1, id);
			statement.setInt(2, blockHeight);

			Map<UUID, AccountProperty> properties = new HashMap<>();
			ResultSet set = statement.executeQuery();
			while (set.next()) {

				String value = set.getString("value");

				Map<String, Object> map = new HashMap<>();
				if (value != null && value.length() != 0) {
					Bencode bencode = new Bencode();
					map = bencode.decode(value.getBytes(), Type.DICTIONARY);
				}

				UUID type = UUID.fromString(set.getString("type"));
				if (type.equals(AccountAttributes.ID) || type.equals(AccountDeposit.ID)
						|| type.equals(AccountBalance.ID)) {

					if (type.equals(AccountDeposit.ID)) {
						map.put("height", set.getInt("height"));
					}

					AccountProperty property = new AccountProperty(type, map);
					properties.put(type, property);

				} else {
					throw new UnsupportedOperationException();
				}

			}
			set.close();
			return new Account(id, properties.values().toArray(new AccountProperty[0]));

		}

	}

	private static boolean validateAccount(IAccount account) {

		try {
			return (account.getID() == Format.MathID.pick(AccountAttributes.getPublicKey(account)));
		} catch (Exception ignore) {
			Loggers.error(MerkleMigrateAction.class, ignore);
			return false;
		}

	}

	private static byte[] createSnapshot(ConnectionProxy connection, Block block, byte[] snapshot) throws SQLException {

		ILedger state = Ledgers.newLeger(connection, snapshot);

		Map<Long, IAccount> updated = new HashMap<>();
		PreparedStatement statement = connection.prepareStatement(
				"select \"type\", \"account\", \"value\", \"blockid\", \"height\" from \"property\" where \"blockid\" = ?");
		synchronized (statement) {

			statement.setLong(1, block.getID());
			ResultSet set = statement.executeQuery();
			while (set.next()) {

				long accountID = set.getLong("account");
				IAccount account = updated.get(accountID);
				if (account == null) {
					account = state.getAccount(accountID);
					if (account == null) {
						account = new Account(accountID);
					}

					if (!updated.containsKey(accountID)) {
						updated.put(accountID, account);
					}
				}

				String value = set.getString("value");
				Map<String, Object> map = new HashMap<>();
				if (value != null && value.length() != 0) {
					Bencode bencode = new Bencode();
					map = bencode.decode(value.getBytes(), Type.DICTIONARY);
				}

				UUID type = UUID.fromString(set.getString("type"));
				if (type.equals(AccountAttributes.ID) || type.equals(AccountDeposit.ID)
						|| type.equals(AccountBalance.ID)) {
					if (type.equals(AccountDeposit.ID)) {
						map.put("height", set.getInt("height"));
					}
					account.putProperty(new AccountProperty(type, map));
				} else {
					throw new UnsupportedOperationException();
				}

			}
			set.close();
		}

		for (Map.Entry<Long, IAccount> entry : updated.entrySet()) {
			IAccount account = entry.getValue();
			if (!validateAccount(account)) {
				throw new AssertionError("Data is corrupted.");
			}
			state.putAccount(account);
		}

		return state.getHash();

	}

}
