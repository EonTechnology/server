package com.exscudo.peer.core.api.impl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.api.IMetadataService;
import com.exscudo.peer.core.api.SalientAttributes;
import com.exscudo.peer.core.blockchain.IBlockchainProvider;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.env.ExecutionContext;
import com.exscudo.peer.core.env.Peer;
import com.exscudo.peer.core.env.PeerInfo;
import com.exscudo.peer.core.storage.Storage;

/**
 * Basic implementation of the {@code IMetadataService} interface
 */
public class SyncMetadataService extends BaseService implements IMetadataService {
    private final ExecutionContext context;
    private final IBlockchainProvider blockchain;
    private final IFork fork;
    private final Storage storage;

    public SyncMetadataService(IFork fork, ExecutionContext context, IBlockchainProvider blockchain, Storage storage) {
        this.context = context;
        this.blockchain = blockchain;
        this.fork = fork;
        this.storage = storage;
    }

    @Override
    public SalientAttributes getAttributes() throws RemotePeerException, IOException {

        SalientAttributes originAttributes = new SalientAttributes();
        originAttributes.setApplication(context.getApplication());
        originAttributes.setVersion(context.getVersion());
        originAttributes.setPeerId(context.getHost().getPeerID());

        Block lastBlock = blockchain.getLastBlock();
        originAttributes.setNetworkID(fork.getGenesisBlockID().toString());
        originAttributes.setFork(fork.getNumber(lastBlock.getTimestamp()));

        originAttributes.setHistoryFromHeight(storage.metadata().getHistoryFromHeight());

        if (context.getHost().getAddress() != null && context.getHost().getAddress().length() > 0) {
            originAttributes.setAnnouncedAddress(context.getHost().getAddress());
        }
        return originAttributes;
    }

    @Override
    public String[] getWellKnownNodes() throws RemotePeerException, IOException {

        Collection<String> wellKnownPeers = new ArrayList<>();

        String[] addresses = context.getPeers().getPeersList();
        for (String address : addresses) {

            PeerInfo peer = context.getPeers().getPeerByAddress(address);
            if (peer != null) {
                if (peer.getState() == PeerInfo.STATE_CONNECTED &&
                        peer.getBlacklistingTime() == 0 &&
                        peer.getAddress() != null &&
                        peer.getAddress().length() > 0 &&
                        !peer.isInner()) {
                    wellKnownPeers.add(peer.getAddress());
                }
            }
        }

        return wellKnownPeers.toArray(new String[0]);
    }

    @Override
    public boolean addPeer(long peerID, String address) throws RemotePeerException, IOException {

        if (peerID == context.getHost().getPeerID()) {
            throw new RemotePeerException("Adding by myself");
        }

        try {

            URL url = new URL("https://" + address);
            if (url.getHost().equals("127.0.0.1") || url.getHost().equals("localhost")) {
                address = getRemoteHost() + ":" + url.getPort();
                url = new URL("https://" + address);
            }

            PeerInfo peerByIP = context.getPeers().getPeerByIP(url.getHost());
            if (peerByIP != null) {
                if (!Objects.equals(peerByIP.getAddress(), address)) {
                    throw new RemotePeerException("Peer IP already registered");
                }
                return true;
            }
        } catch (RemotePeerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RemotePeerException(ex);
        }

        PeerInfo peerInfo = new PeerInfo(0, address, PeerInfo.TYPE_NORMAL);
        Peer peer = new Peer(peerInfo, context.getProxyFactory());
        SalientAttributes attributes = peer.getMetadataService().getAttributes();

        if (attributes.getPeerId() != peerID) {
            throw new RemotePeerException("Different PeerID in attributes");
        }

        if (!fork.getGenesisBlockID().toString().equals(attributes.getNetworkID())) {
            throw new RemotePeerException("Different NetworkID");
        }

        int forkNumber = fork.getNumber(blockchain.getLastBlock().getTimestamp());
        if (attributes.getFork() != forkNumber) {
            throw new RemotePeerException("Different Fork");
        }

        context.getPeers().addPeer(address);

        return true;
    }
}
