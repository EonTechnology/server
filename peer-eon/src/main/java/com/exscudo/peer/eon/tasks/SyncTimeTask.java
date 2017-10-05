package com.exscudo.peer.eon.tasks;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import com.exscudo.peer.core.utils.Loggers;
import com.exscudo.peer.eon.ExecutionContext;

/**
 * Time synchronization by SNTP protocol.
 * 
 */
public final class SyncTimeTask extends BaseTask implements Runnable {
	/**
	 * SNTP UDP port
	 */
	private static final int PORT = 123;
	// TODO: load NTP_SERVER_NAME from configuration.
	private static final String NTP_SERVER_NAME = "pool.ntp.org";
	private static final int TIMEOUT = 10000;

	public SyncTimeTask(ExecutionContext context) {
		super(context);
	}

	@Override
	public void run() {

		try {

			TimeInfo timeInfo = null;

			InetAddress address = null;
			try {
				address = InetAddress.getByName(NTP_SERVER_NAME);
			} catch (UnknownHostException e) {
				throw new IOException(e);
			}

			final NTPUDPClient client = new NTPUDPClient();
			client.setDefaultTimeout(TIMEOUT);
			try {
				client.open();
				timeInfo = client.getTime(address, PORT);

				// compute offset/delay if not already done
				timeInfo.computeDetails();

				Long offset = timeInfo.getOffset();
				if (offset == null) {
					throw new IOException("Failed to calculate offset.");
				}

				// time correction is made if the value difference is more than
				// 1 second
				if (context.getTimeProvider().trySetTimeOffset(offset)) {
					Loggers.trace(SyncTimeTask.class, "The services time was corrected. Value - ({}).", offset);
				}

			} catch (SocketException e) {
				throw new IOException(e);
			} finally {
				client.close();
			}

		} catch (IOException e) {
			Loggers.error(SyncTimeTask.class, "Failed to get current network time.", e);
		} catch (Exception e) {
			Loggers.error(SyncTimeTask.class, e);
		}
	}
}
