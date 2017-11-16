package com.exscudo.peer.eon.tasks;

import com.exscudo.peer.core.utils.Loggers;

public class TimedTask implements Runnable {

	private final Runnable mainTask;

	public TimedTask(Runnable mainTask) {

		this.mainTask = mainTask;
	}
	@Override
	public void run() {
		long begin = System.nanoTime();
		try {
			mainTask.run();
		} finally {
			long diff = System.nanoTime() - begin;
			Loggers.info(mainTask.getClass(), "Timing: {}ms", diff / 1000000.0);
		}

	}
}
