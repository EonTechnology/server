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
import java.util.Objects;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteConfig.TransactionMode;

import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.ForkProvider;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.exceptions.DataAccessException;
import com.exscudo.peer.core.services.IUnitOfWork;
import com.exscudo.peer.store.sqlite.lock.ILockManager;
import com.exscudo.peer.store.sqlite.lock.ILockableObject;
import com.exscudo.peer.store.sqlite.lock.PessimisticLockManager;
import com.exscudo.peer.store.sqlite.utils.BlockHelper;
import com.exscudo.peer.store.sqlite.utils.SettingHelper;
import com.exscudo.peer.store.sqlite.utils.SettingName;

/**
 * Init DB connection.
 * <p>
 * To create instance use {@link Storage#create}
 *
 */
public class Storage {

	private final ConnectionProxy connection;
	private Backlog backlog;

	final ILockManager lockManager;
	final ILockableObject transactionRoot;
	final ILockableObject blockRoot;

	private volatile Block lastBlock;

	public Storage(ConnectionProxy connection) {

		this.connection = connection;
		this.lockManager = new PessimisticLockManager();

		transactionRoot = new ILockableObject() {
			private final Object syncObject = new Object();

			@Override
			public Object getIdentifier() {
				return syncObject;
			}
		};

		blockRoot = new ILockableObject() {
			private final Object syncObject = new Object();

			@Override
			public Object getIdentifier() {
				return syncObject;
			}
		};

		try {
			long genesisBlockID = Long.parseLong(SettingHelper.getValue(connection, SettingName.genesisBlockID), 10);
			String begin = SettingHelper.getValue(connection, SettingName.forkBegin);
			String end = SettingHelper.getValue(connection, SettingName.forkEnd);
			ForkProvider.init(new Fork(genesisBlockID, begin, end));

			long lastID = Long.parseLong(SettingHelper.getValue(connection, SettingName.lastBlockID), 10);
			Block lastBlock = BlockHelper.get(connection, lastID);
			setLastBlock(lastBlock);

		} catch (DataAccessException e) {
			throw new RuntimeException(e);
		}

	}

	public synchronized Block setLastBlock(Block block) {
		this.lastBlock = block;
		SettingHelper.setValue(getConnection(), SettingName.lastBlockID, Long.toString(block.getID()));
		return block;
	}

	public Block getLastBlock() {
		return lastBlock;
	}

	public ConnectionProxy getConnection() {
		return connection;
	}

	public Backlog getBacklog() {
		return backlog;
	}

	public void setBacklog(Backlog backlog) {
		this.backlog = backlog;
	}

	ILockManager getLockManager() {
		return lockManager;
	}

	public LockedObject lockObject(ILockableObject obj) {
		Objects.requireNonNull(obj);

		LockedObject la = new LockedObject(getLockManager(), obj);
		getLockManager().obtainLock(obj);
		return la;
	}

	public LockedObject lockTransactions() {
		return lockObject(transactionRoot);
	}

	public LockedObject lockBlocks() {
		return lockObject(blockRoot);
	}

	public IUnitOfWork createUnitOfWork(Block block) {
		UnitOfWork uow = new UnitOfWork(this);
		try {
			uow.begin(block);
		} catch (Throwable e) {
			uow.rollback();
			throw e;
		}
		return uow;
	}

	public void destroy() throws SQLException {
		if (connection != null)
			connection.getConnection().close();
	}

	//
	// Static members
	//

	public static Storage create(String url) throws ClassNotFoundException, IOException, SQLException {

		return create(url,
				new String[]{"/com/exscudo/eon/data/sqlite/DBv1.sql", "/com/exscudo/eon/data/sqlite/init.sql"});
	}

	public static Storage create(String url, String[] initFiles)
			throws ClassNotFoundException, IOException, SQLException {

		if (url == null) {
			throw new IllegalArgumentException();
		}

		Class.forName("org.sqlite.JDBC");
		SQLiteConfig config = new SQLiteConfig();
		config.setJournalMode(JournalMode.WAL);
		config.setBusyTimeout("5000");
		config.setTransactionMode(TransactionMode.EXCLUSIVE);

		ConnectionProxy conn = new ConnectionProxy(DriverManager.getConnection(url, config.toProperties()));

		Statement statement = conn.getConnection().createStatement();
		statement.executeUpdate("BEGIN IMMEDIATE;");
		int db_version = 0;
		try {
			db_version = Integer.parseInt(SettingHelper.getValue(conn, SettingName.dbVersion), 10);
		} catch (Exception ignored) {
		}

		switch (db_version) {
			case 0 :
				for (String file : initFiles) {
					runSqlScript(conn.getConnection(), statement, file);
				}
			default :
				break;
		}

		statement.executeUpdate("COMMIT;");
		return new Storage(conn);
	}

	private static void runSqlScript(Connection connection, Statement statement, String fileName)
			throws IOException, SQLException {

		InputStream inputStream = connection.getClass().getResourceAsStream(fileName);

		try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			BufferedReader r = new BufferedReader(reader);
			String str = null;
			StringBuilder sb = new StringBuilder(8192);

			while ((str = r.readLine()) != null) {
				sb.append(str);
				sb.append("\n");
			}

			statement.executeUpdate(sb.toString());
		}

	}

	//
	// Nested types
	//

	public static class LockedObject {
		final ILockableObject lockableObject;
		final ILockManager lockManager;

		LockedObject(ILockManager manager, ILockableObject obj) {
			this.lockableObject = obj;
			this.lockManager = manager;
		}

		public void unlock() {
			lockManager.releaseLock(lockableObject);
		}
	}

}
