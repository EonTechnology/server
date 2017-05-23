package com.exscudo.eon.peer.tasks;

import java.io.IOException;

import com.exscudo.eon.StringConstant;
import com.exscudo.eon.exceptions.DecodeException;
import com.exscudo.eon.peer.ExecutionContext;
import com.exscudo.eon.peer.Hallmark;
import com.exscudo.eon.peer.HallmarkProcessor;
import com.exscudo.eon.peer.Peer;
import com.exscudo.eon.peer.Peer.Metadata;
import com.exscudo.eon.peer.Peer.State;
import com.exscudo.eon.peer.contract.MetadataService;
import com.exscudo.eon.peer.contract.SalientAttributes;
import com.exscudo.eon.peer.exceptions.RemotePeerException;
import com.exscudo.eon.utils.Loggers;

/**
 * Supports the number of nodes near to the specified value.
 *
 * If the number of nodes is less than the specified value, then at each call
 * you are connecting to one more node.
 */
public final class PeerConnectTask extends AbstractTask implements Runnable {

	public PeerConnectTask(ExecutionContext context) {
		super(context);
	}

	@Override
	public void run() {

		try {

			if (context.getConnectedPoolSize() < context.getMaxNumberOfConnectedPeers()) {

				Peer peer = context.getAnyPeerToConnect();
				if (peer != null) {

					ExecutionContext.Host host = context.getHost();
					if (peer.getMetadata().peerID == host.getPeerID()) {

						throw new UnsupportedOperationException("Invalid peer id.");

					}

					SalientAttributes remoteAttributes = null;
					try {

						SalientAttributes originAttributes = new SalientAttributes();
						if (host.getAddress() != null && host.getAddress().length() > 0) {
							originAttributes.setAnnouncedAddress(host.getAddress());
						}
						if (host.getHallmark() != null && host.getHallmark().length() > 0) {
							originAttributes.setHallmark(host.getHallmark());
						}
						originAttributes.setApplication(StringConstant.applicationName);
						originAttributes.setPeerId(host.getPeerID());
						originAttributes.setVersion(context.getVersion());

						MetadataService stub = context.createProxy(peer, MetadataService.class);
						remoteAttributes = stub.getAttributes(originAttributes);

					} catch (IOException | RemotePeerException e) {

						synchronized (peer) {

							if (peer.getState() == State.STATE_AMBIGUOUS) {
								peer.setBlacklistingTime(System.currentTimeMillis());
							} else {
								peer.setState(State.STATE_DISCONNECTED);
							}

						}

						Loggers.STREAM.trace(SyncPeerListTask.class,
								">> [" + peer.getAnnouncedAddress() + "] Failed to execute a request.", e);
						Loggers.VERBOSE.debug(SyncPeerListTask.class, "The node is disconnected. \"{}\".",
								peer.getAnnouncedAddress());

						return;
					}

					if (remoteAttributes != null) {

						Metadata metadata = new Metadata(remoteAttributes.getPeerId(),
								remoteAttributes.getApplication(), remoteAttributes.getVersion());
						peer.setMetadata(metadata);

						String hallmark = (String) remoteAttributes.getHallmark();
						try {

							if (hallmark != null) {
								HallmarkProcessor.analyze(peer, Hallmark.parse(hallmark), context);
							}

							peer.setState(State.STATE_CONNECTED);

							Loggers.VERBOSE.info(SyncPeerListTask.class, "The node is connected. \"{}\".",
									peer.getAnnouncedAddress());

						} catch (DecodeException e) {

							peer.setBlacklistingTime(System.currentTimeMillis());

							Loggers.VERBOSE.trace(SyncPeerListTask.class, "Invalid hallmark. ", e);
							Loggers.VERBOSE.info(SyncPeerListTask.class,
									"Invalid hallmark. The node has been added to blacklist. \"{}\" ",
									peer.getAnnouncedAddress());
						}

					}
				}

			}

		} catch (Exception e) {

			Loggers.NOTICE.error(SyncPeerListTask.class, e);
		}

	}

}