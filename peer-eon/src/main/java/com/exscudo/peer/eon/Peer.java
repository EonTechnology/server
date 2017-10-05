package com.exscudo.peer.eon;

import java.util.Objects;

import com.exscudo.peer.core.IPeer;
import com.exscudo.peer.eon.services.IMetadataService;
import com.exscudo.peer.eon.services.ITransactionSynchronizationService;

/**
 * Provides access to the node services.
 *
 */
public class Peer implements IPeer {
	private static final long serialVersionUID = -4020606772258799081L;

	private final PeerInfo pi;
	private final IServiceProxyFactory proxyFactory;

	public Peer(PeerInfo pi, IServiceProxyFactory proxyFactory) {
		this.pi = pi;
		this.proxyFactory = proxyFactory;
	}

	@Override
	public long getPeerID() {
		return pi.getMetadata().getPeerID();
	}

	@Override
	public <TService> TService getService(Class<TService> clazz) {
		Objects.requireNonNull(clazz);
		return proxyFactory.createProxy(pi, clazz);
	}

	public ITransactionSynchronizationService getTransactionSynchronizationService() {
		return proxyFactory.createProxy(pi, ITransactionSynchronizationService.class);
	}

	public IMetadataService getMetadataService() {
		return proxyFactory.createProxy(pi, IMetadataService.class);
	}

	public PeerInfo getPeerInfo() {
		return pi;
	}

	@Override
	public String toString() {
		return pi.getAddress();
	}

}
