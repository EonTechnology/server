package com.exscudo.peer.eon.tasks;

import java.io.IOException;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.core.utils.Loggers;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.Instance;
import com.exscudo.peer.eon.Peer;
import com.exscudo.peer.eon.PeerInfo.Metadata;
import com.exscudo.peer.eon.services.IMetadataService;
import com.exscudo.peer.eon.services.SalientAttributes;

/**
 * Supports a number of connected nodes near to the necessary value.
 * <p>
 * Attempts to connect a random unconnected node. A node must belong to the same
 * generation of hard-forks and a instance of the network as the current feast.
 * Connection is carried out for a period of time, the duration of which is
 * defined in the context. The algorithm for selecting the peer for connection
 * and the duration is described in {@code ExecutionContext}.
 */
public final class PeerConnectTask extends BaseTask implements Runnable {

	public PeerConnectTask(ExecutionContext context) {
		super(context);
	}

	@Override
	public void run() {

		try {

			Instance instance = context.getInstance();
			Peer peer = context.getAnyPeerToConnect();
			if (peer != null) {

				ExecutionContext.Host host = context.getHost();
				if (peer.getPeerInfo().getMetadata().getPeerID() == host.getPeerID()) {
					throw new UnsupportedOperationException("Invalid peer id.");
				}

				SalientAttributes remoteAttributes = null;
				try {

					IMetadataService service = peer.getMetadataService();
					remoteAttributes = service.getAttributes();

				} catch (IOException | RemotePeerException e) {

					context.disablePeer(peer);
					Loggers.trace(PeerConnectTask.class, "Failed to execute a request. Target: " + peer, e);
					Loggers.info(PeerConnectTask.class, "The node is disconnected. \"{}\".", peer);
					return;

				}

				if (remoteAttributes == null) {
					Loggers.info(PeerConnectTask.class, "RemoteAttributes is null. \"{}\".", peer);
					return;
				}

				// Checks the instance of network. Different network instances
				// has different genesis-blocks.
				IFork hardFork = context.getCurrentFork();
				if (!Format.ID.blockId(hardFork.getGenesisBlockID()).equals(remoteAttributes.getNetworkID())) {
					Loggers.info(PeerConnectTask.class, "Different NetworkID. \"{}\".", peer);
					return;
				}

				// Connects only a nodes with known hard-forks.
				int forkNumber = hardFork.getNumber(instance.getBlockchainService().getLastBlock().getTimestamp());
				int forkNumberReal = hardFork.getNumber(context.getCurrentTime());
				if (forkNumber != remoteAttributes.getFork() && forkNumberReal != remoteAttributes.getFork()) {
					Loggers.info(PeerConnectTask.class, "Incorrect fork. \"{}\".", peer);
					return;
				}

				// Saves attributes.
				Metadata metadata = new Metadata(remoteAttributes.getPeerId(), remoteAttributes.getApplication(),
						remoteAttributes.getVersion());
				peer.getPeerInfo().setMetadata(metadata);

				if (metadata.getPeerID() == host.getPeerID()) {
					return;
				}

				// Mark the node as accessible to the connection.
				context.connectPeer(peer);
				Loggers.info(PeerConnectTask.class, "The node is connected. \"{}\".", peer);

			}

		} catch (Exception e) {

			Loggers.error(PeerConnectTask.class, e);
		}

	}

}