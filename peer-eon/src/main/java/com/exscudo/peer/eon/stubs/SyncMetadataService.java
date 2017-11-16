package com.exscudo.peer.eon.stubs;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.services.IBlockchainService;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.Peer;
import com.exscudo.peer.eon.PeerInfo;
import com.exscudo.peer.eon.services.IMetadataService;
import com.exscudo.peer.eon.services.SalientAttributes;

/**
 * Basic implementation of the {@code IMetadataService} interface
 */
public class SyncMetadataService extends BaseService implements IMetadataService {
	private final ExecutionContext context;

	public SyncMetadataService(ExecutionContext context) {
		this.context = context;
	}

	@Override
	public SalientAttributes getAttributes() throws RemotePeerException, IOException {

		SalientAttributes originAttributes = new SalientAttributes();
		originAttributes.setApplication(context.getApplication());
		originAttributes.setVersion(context.getVersion());
		originAttributes.setPeerId(context.getHost().getPeerID());

		Block lastBlock = context.getInstance().getBlockchainService().getLastBlock();
		Fork fork = context.getCurrentFork();
		originAttributes.setNetworkID(Format.ID.blockId(fork.getGenesisBlockID()));
		originAttributes.setFork(fork.getNumber(lastBlock.getTimestamp()));

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
				if (peer.getState() == PeerInfo.STATE_CONNECTED && peer.getBlacklistingTime() == 0
						&& peer.getAddress() != null && peer.getAddress().length() > 0 && !peer.isInner()) {
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

		Fork fork = context.getCurrentFork();

		if (!Format.ID.blockId(fork.getGenesisBlockID()).equals(attributes.getNetworkID())) {
			throw new RemotePeerException("Different NetworkID");
		}

		IBlockchainService blockchainService = context.getInstance().getBlockchainService();

		int forkNumber = fork.getNumber(blockchainService.getLastBlock().getTimestamp());
		if (attributes.getFork() != forkNumber || forkNumber == -1) {
			throw new RemotePeerException("Different Fork");
		}

		context.getPeers().addPeer(address);

		return true;
	}

}
