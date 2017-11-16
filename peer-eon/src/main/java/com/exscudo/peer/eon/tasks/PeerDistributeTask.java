package com.exscudo.peer.eon.tasks;

import com.exscudo.peer.core.utils.Loggers;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.Peer;
import com.exscudo.peer.eon.services.IMetadataService;

/**
 * Distribute current peer address to other peers.
 */
public class PeerDistributeTask extends BaseTask implements Runnable {

	public PeerDistributeTask(ExecutionContext context) {
		super(context);
	}

	@Override
	public void run() {

		try {

			Peer peer = context.getAnyConnectedPeer();

			if (peer != null) {

				IMetadataService service = peer.getMetadataService();
				ExecutionContext.Host host = context.getHost();

				boolean result = service.addPeer(host.getPeerID(), host.getAddress());

				Loggers.info(PeerDistributeTask.class, "Peer adding to \"{}\": {}", peer.getPeerInfo().getAddress(),
						(result ? "OK" : "FAIL"));

			}

		} catch (Exception e) {

			Loggers.error(PeerDistributeTask.class, e);

		}
	}
}
