package com.exscudo.peer.store.sqlite.utils;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.DataAccessException;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.store.sqlite.ConnectionProxy;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Management transactions in DB
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class TransactionHelper {

	static final String SELECT_TRANSACTIONS_SQL = "select \"id\", \"type\", \"version\", \"timestamp\", \"deadline\", \"sender\", \"recipient\", \"fee\", \"referencedTransaction\", \"signature\", \"attachment\", \"block\", \"height\", \"confirmation\" from \"transaction\" ";

	/**
	 * Read transaction from DB row
	 *
	 * @param set row to read
	 * @return transaction from row
	 * @throws SQLException problems with the DB
	 */
	static Transaction getTransactionFromRow(ResultSet set) throws SQLException {

		int type = set.getInt("type");
		int version = set.getInt("version");
		int timestamp = set.getInt("timestamp");
		int deadline = set.getInt("deadline");
		long sender = set.getLong("sender");
		long fee = set.getLong("fee");
		long referencedTransaction = set.getLong("referencedTransaction");
		byte[] signature = Format.convert(set.getString("signature"));

		Map<String, Object> data = null;
		String attachmentText = set.getString("attachment");
		if (attachmentText != null && attachmentText.length() > 0) {
			Bencode bencode = new Bencode();
			data = bencode.decode(attachmentText.getBytes(), Type.DICTIONARY);
		}

		Map<String, Object> confirmations = null;
		String confirmationText = set.getString("confirmation");
		if (confirmationText != null && confirmationText.length() > 0) {
			Bencode bencode = new Bencode();
			confirmations = bencode.decode(confirmationText.getBytes(), Type.DICTIONARY);
		}

		Transaction transaction = new Transaction();
		transaction.setType(type);
		transaction.setVersion(version);
		transaction.setTimestamp(timestamp);
		transaction.setDeadline(deadline);
		transaction.setReference(referencedTransaction);
		transaction.setSenderID(sender);
		transaction.setFee(fee);
		transaction.setData(data);
		transaction.setSignature(signature);
		transaction.setBlock(set.getLong("block"));
		transaction.setHeight(set.getInt("height"));
		transaction.setConfirmations(confirmations);

		return transaction;
	}

	/**
	 * Read transaction from DB
	 *
	 * @param db data connection
	 * @param id transaction id
	 * @return transaction or null if transaction not exist
	 * @throws DataAccessException problems with the DB
	 */
	public static Transaction get(ConnectionProxy db, final long id) throws DataAccessException {

		try {

			PreparedStatement getStatement = db.prepareStatement(SELECT_TRANSACTIONS_SQL + " where \"id\" = ?");
			synchronized (getStatement) {

				getStatement.setLong(1, id);
				ResultSet set = getStatement.executeQuery();

				if (set.next()) {

					Transaction transaction = getTransactionFromRow(set);
					set.close();

					return transaction;
				}
			}

			return null;

		} catch (Exception e) {

			throw new DataAccessException(e);
		}
	}

	/**
	 * Check if transaction exists
	 *
	 * @param db data connection
	 * @param id transaction id
	 * @return true if transaction exists, otherwise false
	 * @throws DataAccessException problems with the DB
	 */
	public static boolean contains(ConnectionProxy db, long id) throws DataAccessException {

		try {

			PreparedStatement containStatement = db
					.prepareStatement("select count(1) from \"transaction\" where \"id\" = ?");
			synchronized (containStatement) {

				containStatement.setLong(1, id);
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

	/**
	 * Save transaction to DB
	 *
	 * @param db          data connection
	 * @param transaction transaction to save
	 * @throws DataAccessException problems with the DB
	 */
	public static void save(ConnectionProxy db, final Transaction transaction) throws DataAccessException {

		try {

			String data = "";
			if (transaction.getData() != null) {
				Bencode bencode = new Bencode();
				byte[] encoded = bencode.encode(transaction.getData());
				data = new String(encoded, bencode.getCharset());
			}

			long recipientID = 0;
			if (transaction.getData() != null && transaction.getData().containsKey("recipient")) {
				recipientID = Format.ID.accountId(transaction.getData().get("recipient").toString());
			}

			if (transaction.getType() == TransactionType.AccountRegistration){
				String accountId = transaction.getData().keySet().iterator().next();
				recipientID = Format.ID.accountId(accountId);
			}

			String confirmations = "";
			if (transaction.getConfirmations() != null) {
				Bencode bencode = new Bencode();
				byte[] encoded = bencode.encode(transaction.getConfirmations());
				confirmations = new String(encoded, bencode.getCharset());
			}

			PreparedStatement saveStatement = db.prepareStatement(
					"INSERT OR REPLACE INTO \"transaction\" (\"id\", \"type\", \"timestamp\", \"deadline\", \"sender\", \"recipient\", \"fee\", \"referencedTransaction\", \"signature\", \"attachment\", \"block\", \"height\", \"version\", \"confirmation\")\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
			synchronized (saveStatement) {
				saveStatement.setLong(1, transaction.getID());
				saveStatement.setInt(2, transaction.getType());
				saveStatement.setInt(3, transaction.getTimestamp());
				saveStatement.setInt(4, transaction.getDeadline());
				saveStatement.setLong(5, transaction.getSenderID());
				saveStatement.setLong(6, recipientID);
				saveStatement.setLong(7, transaction.getFee());
				saveStatement.setLong(8, transaction.getReference());
				saveStatement.setString(9, Format.convert(transaction.getSignature()));
				saveStatement.setString(10, data);
				saveStatement.setLong(11, transaction.getBlock());
				saveStatement.setInt(12, transaction.getHeight());
				saveStatement.setInt(13, transaction.getVersion());
				saveStatement.setString(14, confirmations);


				saveStatement.executeUpdate();
			}
		} catch (Exception e) {

			throw new DataAccessException(e);
		}

	}

	/**
	 * Remove transaction from DB
	 *
	 * @param db data connection
	 * @param id transaction id
	 * @throws DataAccessException problems with the DB
	 */
	public static void remove(ConnectionProxy db, final long id) throws DataAccessException {

		try {

			PreparedStatement removeStatement = db.prepareStatement("delete from \"transaction\" where \"id\" = ?");
			synchronized (removeStatement) {

				removeStatement.setLong(1, id);
				removeStatement.executeUpdate();
			}

		} catch (Exception e) {

			throw new DataAccessException(e);
		}

	}

	/**
	 * Find all transactions for account
	 *
	 * @param db        data connection
	 * @param accountId account id
	 * @return transaction map. Empty if user does not exist or has not sent any
	 * transaction.
	 * @throws DataAccessException problems with the DB
	 */
	public static List<Transaction> findByAccount(ConnectionProxy db, long accountId, long from, int limit)
			throws DataAccessException {

		try {

			PreparedStatement statement = db.prepareStatement(SELECT_TRANSACTIONS_SQL
					+ " where \"recipient\" = ?1 or sender = ?1 order by \"timestamp\" desc, \"id\" LIMIT ?2 OFFSET ?3");
			synchronized (statement) {

				statement.setLong(1, accountId);
				statement.setLong(2, limit);
				statement.setLong(3, from);

				return readTransactionSet(statement);
			}
		} catch (Exception e) {

			throw new DataAccessException(e);
		}
	}

	public static Transaction findRegTransaction(ConnectionProxy db, long accountId) {
		try {

			PreparedStatement statement = db.prepareStatement(SELECT_TRANSACTIONS_SQL
					+ " where \"type\" = ?1 and \"recipient\" = ?2");
			synchronized (statement) {

				statement.setInt(1, TransactionType.AccountRegistration);
				statement.setLong(2, accountId);

				ResultSet set = statement.executeQuery();

				if (set.next()) {

					Transaction transaction = getTransactionFromRow(set);
					set.close();

					return transaction;
				}

				return null;
			}
		} catch (Exception e) {
			throw new DataAccessException(e);
		}
	}

	/**
	 * Read all transactions from PreparedStatement
	 *
	 * @param statement query to read
	 * @return transaction map
	 * @throws SQLException problems with the DB
	 */
	private static List<Transaction> readTransactionSet(PreparedStatement statement) throws SQLException {
		ResultSet set = statement.executeQuery();

		List<Transaction> map = new ArrayList<>();

		while (set.next()) {
			Transaction transaction = getTransactionFromRow(set);
			map.add(transaction);
		}

		set.close();
		return map;
	}

}
