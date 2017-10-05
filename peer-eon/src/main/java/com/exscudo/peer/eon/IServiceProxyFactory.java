package com.exscudo.peer.eon;

/**
 * Factory for creating a connection to the service on remote node using the
 * current transport protocol.
 *
 */
public interface IServiceProxyFactory {

	/**
	 * Creating a stub to the specified service on remote.
	 * 
	 * @param peer
	 *            parameter specifies the node. can not be null.
	 * @param clazz
	 *            parameter specifies the service. can not be null.
	 * @return stub
	 */
	<TService> TService createProxy(PeerInfo peer, Class<TService> clazz);

}
