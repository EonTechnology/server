package com.exscudo.eon.bot;

import java.io.IOException;

import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.eon.TimeProvider;

/**
 * Current peer time service
 */
public class TimeService {
	private final TimeProvider timeProvider;

	public TimeService(TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	/**
	 * Get current peer time service
	 * 
	 * @return unix timestamp
	 * @throws RemotePeerException
	 * @throws IOException
	 */
	public long get() throws RemotePeerException, IOException {
		return timeProvider.get();
	}

}
