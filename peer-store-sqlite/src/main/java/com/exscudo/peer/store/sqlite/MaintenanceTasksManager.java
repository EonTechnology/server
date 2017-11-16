package com.exscudo.peer.store.sqlite;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.exscudo.peer.eon.tasks.TimedTask;
import com.exscudo.peer.store.sqlite.tasks.AnalyzeTask;
import com.exscudo.peer.store.sqlite.utils.DatabaseHelper;

// TODO: remove self ScheduledExecutorService
/**
 * Task runner for internal DB needs.
 */
public class MaintenanceTasksManager {

	private ScheduledExecutorService scheduledThreadPool;
	private Storage connector;

	public static MaintenanceTasksManager run(Storage connector) {

		MaintenanceTasksManager o = new MaintenanceTasksManager();

		final ConnectionProxy connection = connector.getConnection();
		DatabaseHelper.beginTransaction(connection, "TaskManager");
		o.scheduledThreadPool = Executors.newScheduledThreadPool(1);
		o.scheduledThreadPool.scheduleWithFixedDelay(new TimedTask(new AnalyzeTask(connector)), 0, 10,
				TimeUnit.SECONDS);
		o.connector = connector;

		return o;
	}

	public void destroy() {

		// TODO: run before DB closing
		// final ConnectionProxy connection = connector.getConnection();
		// DatabaseHelper.commitTransaction(connection, "TaskManager");
		scheduledThreadPool.shutdownNow();

	}
}
