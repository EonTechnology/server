package com.exscudo.eon.peer.tasks;

import com.exscudo.eon.peer.ExecutionContext;
import com.exscudo.eon.peer.Peer;
import com.exscudo.eon.peer.Peer.State;
import com.exscudo.eon.utils.Loggers;

/**
 * Removes nodes from the blacklist after the blocking time has expired.
 *
 */
public final class BlackListPeerRemoveTask extends AbstractTask implements Runnable {

	public BlackListPeerRemoveTask(ExecutionContext context) {
		super(context);
	}

	@Override
	public void run() {

		try {

			long curTime = System.currentTimeMillis();

			String[] wellKnownPeers = context.peers.getPeersList();
			for (String address : wellKnownPeers) {
				if (address == null)
					continue;

				Peer peer = context.peers.getPeerByAddress(address);
				if (peer != null) {

					synchronized (peer) {

						if (peer.getBlacklistingTime() > 0
								&& peer.getBlacklistingTime() + context.getBlacklistingPeriod() <= curTime) {

							peer.setState(State.STATE_AMBIGUOUS);
							peer.setBlacklistingTime(0);

							Loggers.VERBOSE.info(BlackListPeerRemoveTask.class,
									"Peer \"{}\" has been removed from blacklist.", peer.getAnnouncedAddress());
						}

					}
				}

			}

		} catch (Exception e) {

			Loggers.NOTICE.error(BlackListPeerRemoveTask.class, e);
		}
	}

}