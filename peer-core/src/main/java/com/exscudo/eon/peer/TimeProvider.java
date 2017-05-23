package com.exscudo.eon.peer;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicLong;

public class TimeProvider {

	private static final long epochBeginning;
	private static AtomicLong offsetTime = new AtomicLong(0);

	static {

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.ZONE_OFFSET, 0);
		calendar.set(Calendar.YEAR, 2016);
		calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		epochBeginning = calendar.getTimeInMillis();
	}

	/**
	 * Provides access to the epoch time.
	 * 
	 * @return
	 */
	public static int getEpochTime() {

		long time = System.currentTimeMillis() + offsetTime.get();
		return (int) ((time - epochBeginning + 500) / 1000);

	}

	/**
	 * Corrects the time offset if the value difference is more than minimal
	 * step.
	 * 
	 * @param currOffset
	 * @return
	 */
	public synchronized static boolean trySetTimeOffset(long currOffset) {

		if (Math.abs(currOffset - offsetTime.get()) > 1000) {

			offsetTime.set(currOffset);
			return true;
		}

		return false;

	}

}
