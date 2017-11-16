package com.exscudo.eon.jsonrpc.proxy;

import java.io.IOException;

import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.eon.IServiceProxyFactory;
import com.exscudo.peer.eon.services.IMetadataService;
import com.exscudo.peer.eon.services.SalientAttributes;

/**
 * Proxy for {@code IMetadataService} on remote peer
 * <p>
 * To create use {@link IServiceProxyFactory#createProxy}
 *
 * @see IMetadataService
 */
public class MetadataServiceProxy extends PeerServiceProxy implements IMetadataService {

	@Override
	public SalientAttributes getAttributes() throws RemotePeerException, IOException {
		return doRequest("getAttributes", new Object[0], SalientAttributes.class);
	}

	@Override
	public String[] getWellKnownNodes() throws RemotePeerException, IOException {
		return doRequest("getWellKnownNodes", new Object[0], String[].class);
	}

	@Override
	public boolean addPeer(long peerID, String address) throws RemotePeerException, IOException {
		return doRequest("addPeer", new Object[]{peerID, address}, Boolean.class);
	}
}
