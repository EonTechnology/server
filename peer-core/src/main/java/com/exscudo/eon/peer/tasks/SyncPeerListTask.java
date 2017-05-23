package com.exscudo.eon.peer.tasks;

import java.io.IOException;
import java.net.URL;

import com.exscudo.eon.peer.ExecutionContext;
import com.exscudo.eon.peer.Peer;
import com.exscudo.eon.peer.Peer.State;
import com.exscudo.eon.peer.contract.MetadataService;
import com.exscudo.eon.peer.exceptions.RemotePeerException;
import com.exscudo.eon.utils.Loggers;

/**
 * Synchronizes the list of well-known nodes with a random node.
 *
 */
public final class SyncPeerListTask extends AbstractTask implements Runnable {

	public SyncPeerListTask(ExecutionContext context) {
		super(context);
	}

	@Override
	public void run() {

		try {

			Peer peer = context.getAnyConnectedPeer();
			if (peer != null) {

				String[] addresses = null;

				try {

					MetadataService stub = context.createProxy(peer, MetadataService.class);
					addresses = stub.getWellKnownNodes();

				} catch (RemotePeerException | IOException e) {

					synchronized (peer) {

						if (peer.getState() == State.STATE_AMBIGUOUS) {
							peer.setBlacklistingTime(System.currentTimeMillis());
						} else {
							peer.setState(State.STATE_DISCONNECTED);
						}

					}

					Loggers.STREAM.trace(SyncPeerListTask.class,
							">> [" + peer.getAnnouncedAddress() + "] Failed to execute a request.", e);
					Loggers.VERBOSE.info(SyncPeerListTask.class, "The node is disconnected. \"{}\".",
							peer.getAnnouncedAddress());

					return;
				}

				if (addresses != null) {
					for (String address : addresses) {

						address = address.trim();
						if (address.length() > 0) {

							String myAddress = context.getHost().getAddress();
							if (myAddress != null && myAddress.length() > 0 && myAddress.equals(address)) {
								continue;
							}

							try {
								new URL("http://" + address);
							} catch (Exception ignore) {
								continue;
							}

							String announcedAddress = address;
							try {
								new URL("http://" + announcedAddress);
							} catch (Exception e) {
								announcedAddress = "";
							}

							if (address.equals("localhost") || address.equals("127.0.0.1")
									|| address.equals("0:0:0:0:0:0:0:1")) {
								continue;
							}

							Peer newPeer = context.peers.addPeer(address, announcedAddress);

							Loggers.VERBOSE.info(SyncPeerListTask.class, "Added new peer \"{}\".",
									newPeer.getAnnouncedAddress());
						}
					}
				}
			}

		} catch (Exception e) {

			Loggers.NOTICE.error(SyncPeerListTask.class, e);

		}

	}
}