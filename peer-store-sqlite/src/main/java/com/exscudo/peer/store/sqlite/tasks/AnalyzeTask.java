package com.exscudo.peer.store.sqlite.tasks;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.utils.Loggers;
import com.exscudo.peer.store.sqlite.ConnectionProxy;
import com.exscudo.peer.store.sqlite.Storage;
import com.exscudo.peer.store.sqlite.Storage.LockedObject;
import com.exscudo.peer.store.sqlite.utils.DatabaseHelper;

/**
 * Management of top-level transaction in DB. SQLite speed up when transaction
 * active.
 */
public class AnalyzeTask implements Runnable {
	private final Storage connector;
	private final boolean useTransactionAlways = true;

	private long lastBlockId = 0;

	public AnalyzeTask(Storage connector) {
		this.connector = connector;
	}

	@Override
	public void run() {

		try {

			Block lastBlock = connector.getLastBlock();
			if (lastBlockId != lastBlock.getID()) {

				int time = (int) ((System.currentTimeMillis() + 500L) / 1000L);

				if (time > lastBlock.getTimestamp() + Constant.BLOCK_PERIOD * 2 / 3) {

					LockedObject blockRoot = connector.lockBlocks();
					LockedObject txRoot = connector.lockTransactions();

					try {

						Loggers.info(AnalyzeTask.class, "TaskManager.run");

						ConnectionProxy connection = connector.getConnection();
						DatabaseHelper.analyze(connection);
						if (useTransactionAlways) {
							DatabaseHelper.commitTransaction(connection, "TaskManager");
							DatabaseHelper.beginTransaction(connection, "TaskManager");
						}
						lastBlockId = lastBlock.getID();
						Loggers.info(AnalyzeTask.class, "TaskManager.run END");

					} finally {
						blockRoot.unlock();
						txRoot.unlock();
					}
				}
			}

		} catch (Exception e) {
			Loggers.error(AnalyzeTask.class, "Unable to perform task.", e);
		}

	}

}
