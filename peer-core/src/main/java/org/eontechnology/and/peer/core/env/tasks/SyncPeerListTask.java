package org.eontechnology.and.peer.core.env.tasks;

import java.io.IOException;
import java.net.URL;

import org.eontechnology.and.peer.core.api.IMetadataService;
import org.eontechnology.and.peer.core.common.Loggers;
import org.eontechnology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechnology.and.peer.core.env.ExecutionContext;
import org.eontechnology.and.peer.core.env.Peer;
import org.eontechnology.and.peer.core.env.PeerInfo;

/**
 * Synchronizes the list of well-known nodes with a random connected node.
 * <p>
 * At the fist step, a randomly node is selected (
 * {@link ExecutionContext#getAnyConnectedPeer}). Next,
 * task is requested to list the nodes connected by the services node. After,
 * the resulting list is added to the known address pool on the current node.
 */
public final class SyncPeerListTask implements Runnable {

    private final ExecutionContext context;

    public SyncPeerListTask(ExecutionContext context) {

        this.context = context;
    }

    @Override
    public void run() {

        try {

            Peer peer = context.getAnyConnectedPeer();
            if (peer != null) {

                String[] addresses = null;
                try {

                    IMetadataService service = peer.getMetadataService();
                    addresses = service.getWellKnownNodes();
                } catch (RemotePeerException | IOException e) {

                    context.disablePeer(peer);
                    Loggers.trace(SyncPeerListTask.class, "Failed to execute a request. Target: " + peer, e);
                    Loggers.info(SyncPeerListTask.class, "The node is disconnected. \"{}\".", peer);
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
                                new URL("https://" + address);
                            } catch (Exception ignore) {
                                continue;
                            }

                            if (context.getPeers().getPeerByIP(address) == null) {
                                PeerInfo newPeer = context.getPeers().addPeer(address);

                                Loggers.info(SyncPeerListTask.class, "Added new peer \"{}\".", newPeer.getAddress());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {

            Loggers.error(SyncPeerListTask.class, e);
        }
    }
}