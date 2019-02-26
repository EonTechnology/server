package org.eontechology.and.peer.core.env.tasks;

import java.io.IOException;

import org.eontechology.and.peer.core.IFork;
import org.eontechology.and.peer.core.api.IMetadataService;
import org.eontechology.and.peer.core.api.SalientAttributes;
import org.eontechology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechology.and.peer.core.common.ITimeProvider;
import org.eontechology.and.peer.core.common.Loggers;
import org.eontechology.and.peer.core.common.exceptions.RemotePeerException;
import org.eontechology.and.peer.core.env.ExecutionContext;
import org.eontechology.and.peer.core.env.Peer;
import org.eontechology.and.peer.core.env.PeerInfo;

/**
 * Supports a number of connected nodes near to the necessary value.
 * <p>
 * Attempts to connect a random unconnected node. A node must belong to the same
 * generation of hard-forks and a instance of the network as the current feast.
 * Connection is carried out for a period of time, the duration of which is
 * defined in the context. The algorithm for selecting the peer for connection
 * and the duration is described in {@code ExecutionContext}.
 */
public final class PeerConnectTask implements Runnable {

    private final IFork fork;
    private final ExecutionContext context;
    private final IBlockchainProvider blockchain;
    private final ITimeProvider timeProvider;

    public PeerConnectTask(IFork fork,
                           ExecutionContext context,
                           ITimeProvider timeProvider,
                           IBlockchainProvider blockchain) {

        this.fork = fork;
        this.context = context;
        this.blockchain = blockchain;
        this.timeProvider = timeProvider;
    }

    @Override
    public void run() {

        try {

            if (context.getConnectedPeerCount() >= context.getConnectedPoolSize()) {
                return;
            }

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
                if (!fork.getGenesisBlockID().toString().equals(remoteAttributes.getNetworkID())) {
                    Loggers.info(PeerConnectTask.class, "Different NetworkID. \"{}\".", peer);
                    return;
                }

                // Connects only a nodes with known hard-forks.
                int forkNumber = fork.getNumber(blockchain.getLastBlock().getTimestamp());
                int forkNumberReal = fork.getNumber(timeProvider.get());
                if (forkNumber != remoteAttributes.getFork() && forkNumberReal != remoteAttributes.getFork()) {
                    Loggers.info(PeerConnectTask.class, "Incorrect fork. \"{}\".", peer);
                    return;
                }

                // Saves attributes.
                PeerInfo.Metadata metadata = new PeerInfo.Metadata(remoteAttributes.getPeerId(),
                                                                   remoteAttributes.getApplication(),
                                                                   remoteAttributes.getVersion(),
                                                                   remoteAttributes.getHistoryFromHeight());
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