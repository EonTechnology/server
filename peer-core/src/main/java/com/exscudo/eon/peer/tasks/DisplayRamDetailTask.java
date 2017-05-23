package com.exscudo.eon.peer.tasks;

import com.exscudo.eon.utils.Loggers;

public final class DisplayRamDetailTask extends AbstractTask implements Runnable {
	private static final double SIZE_UNIT = 1024.0 * 1024.0;

	public DisplayRamDetailTask() {
		super(null);
	}

	@Override
	public void run() {

		try {

			double freeMemory = Runtime.getRuntime().freeMemory() / SIZE_UNIT;
			double totalMemory = Runtime.getRuntime().totalMemory() / SIZE_UNIT;
			double maxMemory = Runtime.getRuntime().maxMemory() / SIZE_UNIT;

			Loggers.DIAGNOSTIC.info(DisplayRamDetailTask.class,
					String.format("Used memory: %.1f MB of %.1f MB (%.1f MB maximum)", totalMemory - freeMemory,
							totalMemory, maxMemory));

			if (freeMemory * 100 / totalMemory < 10) {
				Loggers.NOTICE.warning(DisplayRamDetailTask.class, "Used more than 90 percent of available memory.");
			}

		} catch (Exception e) {

			Loggers.DIAGNOSTIC.error(DisplayRamDetailTask.class, "Failed to display ram info.", e);
		}
	}

}
