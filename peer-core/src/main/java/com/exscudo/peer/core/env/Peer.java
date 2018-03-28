package com.exscudo.peer.core.env;

import java.util.Objects;

import com.exscudo.peer.core.api.IBlockSynchronizationService;
import com.exscudo.peer.core.api.IMetadataService;
import com.exscudo.peer.core.api.ISnapshotSynchronizationService;
import com.exscudo.peer.core.api.ITransactionSynchronizationService;

/**
 * Provides access to the env services.
 */
public class Peer {
    private static final long serialVersionUID = -4020606772258799081L;

    private final PeerInfo pi;
    private final IServiceProxyFactory proxyFactory;

    public Peer(PeerInfo pi, IServiceProxyFactory proxyFactory) {
        this.pi = pi;
        this.proxyFactory = proxyFactory;
    }

    public long getPeerID() {
        return pi.getMetadata().getPeerID();
    }

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

    public IBlockSynchronizationService getBlockSynchronizationService() {
        return proxyFactory.createProxy(pi, IBlockSynchronizationService.class);
    }

    public ISnapshotSynchronizationService getSnapshotSynchronizationService() {
        return proxyFactory.createProxy(pi, ISnapshotSynchronizationService.class);
    }
}
