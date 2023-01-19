package org.eontechnology.and.peer.core.env;

/**
 * Factory for creating a connection to the service on remote env using the current transport
 * protocol.
 */
public interface IServiceProxyFactory {

  /**
   * Creating a stub to the specified service on remote.
   *
   * @param peer parameter specifies the env. can not be null.
   * @param clazz parameter specifies the service. can not be null.
   * @return stub
   */
  <TService> TService createProxy(PeerInfo peer, Class<TService> clazz);
}
