package com.exscudo.peer.store.sqlite.utils;

import java.io.EOFException;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.DataAccessException;
import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.store.sqlite.ConnectionProxy;

/**
 * Manage block in DB
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class BlockHelper {

	/**
	 * Read block from DB
	 * 
	 * @param db
	 *            data connection
	 * @param id
	 *            block id to read
	 * @return block from DB
	 * @throws DataAccessException
	 *             problems with the DB
	 */
	public static Block get(ConnectionProxy db, final long id) throws DataAccessException {

		try {

			PreparedStatement getStatement = db.prepareStatement(
					"select \"version\", \"timestamp\", \"previousBlock\", \"generator\", \"generationSignature\", \"blockSignature\", \"height\", \"nextBlock\", \"cumulativeDifficulty\" from \"block\" where \"id\" = ?");

			ResultSet set;

			synchronized (getStatement) {
				getStatement.setLong(1, id);
				set = getStatement.executeQuery();

				if (set.next()) {

					int version = set.getInt("version");
					int timestamp = set.getInt("timestamp");
					long previousBlock = set.getLong("previousBlock");
					long generator = set.getLong("generator");
					byte[] generationSignature = Format.convert(set.getString("generationSignature"));
					byte[] signature = Format.convert(set.getString("blockSignature"));

					Block block = new DbBlock(db);
					block.setVersion(version);
					block.setTimestamp(timestamp);
					block.setPreviousBlock(previousBlock);
					block.setGenerationSignature(generationSignature);
					block.setSenderID(generator);
					block.setSignature(signature);

					block.setHeight(set.getInt("height"));
					block.setNextBlock(set.getLong("nextBlock"));
					block.setCumulativeDifficulty(new BigInteger(set.getString("cumulativeDifficulty")));
					set.close();

					return block;
				}
			}

			set.close();
			return null;

		} catch (Exception e) {
			throw new DataAccessException(e);
		}
	}

	/**
	 * Save block to DB
	 *
	 * @param db
	 *            data connection
	 * @param block
	 *            Block to save
	 * @throws DataAccessException
	 *             problems with the DB
	 */
	public static void save(ConnectionProxy db, final Block block) throws DataAccessException {

		try {

			PreparedStatement saveStatement = db.prepareStatement(
					"INSERT OR REPLACE INTO \"block\" (\"id\", \"version\", \"timestamp\", \"previousBlock\", \"generator\", \"generationSignature\", \"blockSignature\", \"height\", \"nextBlock\", \"cumulativeDifficulty\")\n VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
			synchronized (saveStatement) {

				saveStatement.setLong(1, block.getID());
				saveStatement.setInt(2, block.getVersion());
				saveStatement.setInt(3, block.getTimestamp());
				saveStatement.setLong(4, block.getPreviousBlock());
				saveStatement.setLong(5, block.getSenderID());
				saveStatement.setString(6, Format.convert(block.getGenerationSignature()));
				saveStatement.setString(7, Format.convert(block.getSignature()));
				saveStatement.setInt(8, block.getHeight());
				saveStatement.setLong(9, block.getNextBlock());
				saveStatement.setString(10, block.getCumulativeDifficulty().toString());
				saveStatement.executeUpdate();

				PreparedStatement savePropsStatement = db.prepareStatement(
						"INSERT OR REPLACE INTO \"property\"(\"type\", \"account\", \"value\", \"blockid\", \"height\")\n VALUES(?, ?, ?, ?, ?);");
				synchronized (savePropsStatement) {

					if (!(block instanceof DbBlock) || ((DbBlock) block).isAccPropsLoaded()) {
						for (AccountProperty i : block.getAccProps()) {
							String data = "";
							if (i.getData() != null) {
								Bencode bencode = new Bencode();
								byte[] encoded = bencode.encode(i.getData());
								data = new String(encoded, bencode.getCharset());
							}

							savePropsStatement.setString(1, i.getType().toString());
							savePropsStatement.setLong(2, i.getAccountID());
							savePropsStatement.setString(3, data);
							savePropsStatement.setLong(4, block.getID());
							savePropsStatement.setInt(5, i.getHeight());
							savePropsStatement.executeUpdate();
						}
					}
				}
			}

		} catch (Exception e) {
			throw new DataAccessException(e);
		}
	}

	/**
	 * Remove block from DB
	 * 
	 * @param db
	 *            data connection
	 * @param id
	 *            block id to remove
	 * @throws DataAccessException
	 *             problems with the DB
	 */
	public static void remove(ConnectionProxy db, final long id) throws DataAccessException {

		try {

			PreparedStatement removeStatement = db.prepareStatement("delete from \"block\" where \"id\" = ?");
			synchronized (removeStatement) {

				removeStatement.setLong(1, id);
				removeStatement.executeUpdate();

				PreparedStatement removePropsStatement = db
						.prepareStatement("delete from \"property\" where \"blockid\" = ?");
				synchronized (removePropsStatement) {
					removePropsStatement.setLong(1, id);
					removePropsStatement.executeUpdate();
				}

				PreparedStatement removeTransStatement = db
						.prepareStatement("delete from \"transaction\" where \"block\" = ?");

				synchronized (removeTransStatement) {
					removeTransStatement.setLong(1, id);
					removeTransStatement.executeUpdate();
				}
			}

		} catch (Exception e) {
			throw new DataAccessException(e);
		}
	}

	/**
	 * Read block history from DB
	 * 
	 * @param db
	 *            data connection
	 * @param begin
	 *            begin block height
	 * @param end
	 *            end block height
	 * @return sorted by height list of block id
	 * @throws DataAccessException
	 *             problems with the DB
	 */
	public static long[] getBlockLinkedList(ConnectionProxy db, int begin, int end) throws DataAccessException {

		try {

			PreparedStatement getListStatement = db.prepareStatement(
					"SELECT \"id\", \"nextBlock\", \"previousBlock\" FROM \"block\" where \"height\" BETWEEN ? and ? order by \"height\"");

			LinkedList<Long> list = new LinkedList<>();
			synchronized (getListStatement) {

				getListStatement.setInt(1, begin);
				getListStatement.setInt(2, end);

				ResultSet set = getListStatement.executeQuery();
				while (set.next()) {
					list.add(set.getLong(1));
				}
				set.close();

			}

			long[] result = new long[list.size()];
			int i = 0;
			for (Long item : list) {
				result[i++] = item;
			}

			return result;

		} catch (Exception e) {
			throw new DataAccessException(e);
		}
	}

	/**
	 * Read block history from DB generated by specified account
	 *
	 * @param db
	 *            data connection
	 * @param peerAccountID
	 *            generator id
	 * @param time
	 *            time limit for read from history
	 * @return sorted by height list of block id
	 * @throws DataAccessException
	 *             problems with the DB
	 */
	public static long[] getBlockListByPeerAccount(ConnectionProxy db, long peerAccountID, int time)
			throws DataAccessException {

		try {

			PreparedStatement statement = db.prepareStatement(
					"SELECT \"id\", \"nextBlock\", \"previousBlock\" FROM \"block\" where \"generator\" = ? and \"timestamp\" >= ? order by \"height\" desc");

			LinkedList<Long> list = new LinkedList<>();
			synchronized (statement) {

				statement.setLong(1, peerAccountID);
				statement.setInt(2, time);

				ResultSet set = statement.executeQuery();
				while (set.next()) {
					list.add(set.getLong(1));
				}
				set.close();

			}

			long[] result = new long[list.size()];
			int i = 0;
			for (Long item : list) {
				result[i++] = item;
			}

			return result;

		} catch (Exception e) {
			throw new DataAccessException(e);
		}
	}

	/**
	 * Get block position in blockchain
	 *
	 * @param db
	 *            data connection
	 * @param id
	 *            block id
	 * @return block height or -1 if block not exist
	 * @throws DataAccessException
	 *             problems with the DB
	 */
	public static int getHeight(ConnectionProxy db, final long id) {
		try {

			PreparedStatement statement = db.prepareStatement("SELECT \"height\" FROM \"block\" where \"id\" = ?");
			int h = -1;
			synchronized (statement) {

				statement.setLong(1, id);

				ResultSet set = statement.executeQuery();
				while (set.next()) {
					h = set.getInt(1);
				}
				set.close();

			}

			return h;

		} catch (Exception e) {
			throw new DataAccessException(e);
		}
	}

	/**
	 * Read transactions for block
	 *
	 * @param db
	 *            data connection
	 * @param id
	 *            block id
	 * @return Transaction list
	 * @throws DataAccessException
	 *             problems with the DB
	 */
	static List<Transaction> getTransactions(ConnectionProxy db, final long id) throws SQLException, EOFException {

		try {

			PreparedStatement getTransactionStatement = db
					.prepareStatement(TransactionHelper.SELECT_TRANSACTIONS_SQL + " where \"block\" = ?");
			synchronized (getTransactionStatement) {
				getTransactionStatement.setLong(1, id);

				List<Transaction> transactions = new ArrayList<>();

				ResultSet tranSet = getTransactionStatement.executeQuery();
				while (tranSet.next()) {
					Transaction transaction = TransactionHelper.getTransactionFromRow(tranSet);
					transactions.add(transaction);
				}
				tranSet.close();

				return transactions;
			}

		} catch (Exception e) {
			throw new DataAccessException(e);
		}

	}

	/**
	 * Read account properties for block
	 *
	 * @param db
	 *            data connection
	 * @param id
	 *            block id
	 * @return properties list
	 * @throws DataAccessException
	 *             problems with the DB
	 */
	static AccountProperty[] findAccProps(ConnectionProxy db, long id) throws SQLException {

		PreparedStatement statement = db.prepareStatement(
				"select \"type\", \"account\", \"value\", \"blockid\", \"height\" from \"property\" where \"blockid\" = ?");
		synchronized (statement) {

			ArrayList<AccountProperty> list = new ArrayList<>();
			statement.setLong(1, id);
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
