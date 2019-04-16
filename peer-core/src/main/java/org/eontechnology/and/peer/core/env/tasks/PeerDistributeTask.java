package org.eontechnology.and.peer.core.env.tasks;

import org.eontechnology.and.peer.core.api.IMetadataService;
import org.eontechnology.and.peer.core.common.Loggers;
import org.eontechnology.and.peer.core.env.ExecutionContext;
import org.eontechnology.and.peer.core.env.Peer;

/**
 * Distribute current peer address to other peers.
 */
public class PeerDistributeTask implements Runnable {

    private final ExecutionContext context;

    public PeerDistributeTask(ExecutionContext context) {
        this.context = context;
    }

    @Override
    public void run() {

        try {

            Peer peer = context.getAnyConnectedPeer();

            if (peer != null) {

                IMetadataService service = peer.getMetadataService();
                ExecutionContext.Host host = context.getHost();

                boolean result = service.addPeer(host.getPeerID(), host.getAddress());

                Loggers.info(PeerDistributeTask.class,
                             "Peer adding to \"{}\": {}",
                             peer.getPeerInfo().getAddress(),
                             (result ? "OK" : "FAIL"));
            }
        } catch (Exception e) {

            Loggers.error(PeerDistributeTask.class, e);
        }
    }
}
