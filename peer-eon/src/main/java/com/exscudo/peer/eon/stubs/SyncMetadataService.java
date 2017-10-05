package com.exscudo.peer.eon.stubs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.exscudo.peer.core.Fork;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.ExecutionContext;
import com.exscudo.peer.eon.PeerInfo;
import com.exscudo.peer.eon.services.IMetadataService;
import com.exscudo.peer.eon.services.SalientAttributes;

/**
 * Basic implementation of the {@code IMetadataService} interface
 *
 */
public class SyncMetadataService implements IMetadataService {
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

		Fork fork = context.getCurrentFork();
		originAttributes.setNetworkID(Format.ID.blockId(fork.getGenesisBlockID()));
		originAttributes
				.setFork(fork.getNumber(context.getInstance().getBlockchainService().getLastBlock().getTimestamp()));

		if (context.getHost().getAddress() != null && context.getHost().getAddress().length() > 0) {
			originAttributes.setAnnouncedAddress(context.getHost().getAddress());
		}
		return originAttributes;

	}

	@Override
	public String[] getWellKnownNodes() throws RemotePeerException, IOException {

		Collection<String> wellKnownPeers = new ArrayList<String>();

		String[] addresses = context.getPeers().getPeersList();
		for (String address : addresses) {

			PeerInfo peer = context.getPeers().getPeerByAddress(address);
			if (peer != null) {
				if (peer.getState() == PeerInfo.State.STATE_CONNECTED && peer.getBlacklistingTime() == 0
						&& peer.getAddress() != null && peer.getAddress().length() > 0 && !peer.isInner()) {
					wellKnownPeers.add(peer.getAddress());
				}
			}
		}

		return wellKnownPeers.toArray(new String[0]);
	}

}
