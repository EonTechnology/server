package com.exscudo.peer.store.sqlite;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.exceptions.DataAccessException;
import com.exscudo.peer.core.services.IUnitOfWork;
import com.exscudo.peer.store.sqlite.lock.ILockManager;
import com.exscudo.peer.store.sqlite.lock.ILockableObject;
import com.exscudo.peer.store.sqlite.lock.PessimisticLockManager;
import com.exscudo.peer.store.sqlite.utils.BlockHelper;
import com.exscudo.peer.store.sqlite.utils.SettingHelper;
import com.exscudo.peer.store.sqlite.utils.SettingName;
import org.sqlite.SQLiteConfig;

/**
 * Init DB connection.
 * <p>
 * To create instance use {@link Storage#create}
 */
public class Storage {

	private final ConnectionProxy connection;
	private Backlog backlog;

	final ILockManager lockManager;
	final ILockableObject transactionRoot;
	final ILockableObject blockRoot;

	private volatile Block lastBlock;

	public Storage(Connection conn) {

		this.connection = new ConnectionProxy(conn);
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

	public synchronized Block getLastBlock() {
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

	public IUnitOfWork createUnitOfWork(Block block, IFork fork) {
		UnitOfWork uow = new UnitOfWork(this, fork);
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
	public static Storage create(String url) throws IOException, ClassNotFoundException, SQLException {
		return create(url, new Initializer());
	}

	public static Storage create(String url, IInitializer loader)
			throws ClassNotFoundException, IOException, SQLException {
		Objects.requireNonNull(url);
		Objects.requireNonNull(loader);

		Class.forName("org.sqlite.JDBC");
		SQLiteConfig config = new SQLiteConfig();
		config.setJournalMode(SQLiteConfig.JournalMode.WAL);
		config.setBusyTimeout("5000");
		config.setTransactionMode(SQLiteConfig.TransactionMode.EXCLUSIVE);

		Connection connection = DriverManager.getConnection(url, config.toProperties());
		loader.initialize(connection);
		return new Storage(connection);
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
