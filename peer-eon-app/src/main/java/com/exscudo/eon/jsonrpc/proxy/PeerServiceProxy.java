package com.exscudo.eon.jsonrpc.proxy;

import java.io.IOException;

import com.exscudo.eon.jsonrpc.JrpcServiceProxy;
import com.exscudo.peer.core.exceptions.RemotePeerException;

/**
 * Proxy for remote peer services
 */
public abstract class PeerServiceProxy {
	protected JrpcServiceProxy proxy;
	protected String serviceName;

	public void setProxy(JrpcServiceProxy proxy) {
		this.proxy = proxy;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public <TService> TService doRequest(String method, Object[] params, Class<TService> clazz)
			throws IOException, RemotePeerException {
		return proxy.post(serviceName + "." + method, params, clazz);
	}
}
