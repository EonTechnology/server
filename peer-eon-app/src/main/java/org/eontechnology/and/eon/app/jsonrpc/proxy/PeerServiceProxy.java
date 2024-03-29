package org.eontechnology.and.eon.app.jsonrpc.proxy;

import java.io.IOException;
import org.eontechnology.and.jsonrpc.JrpcServiceProxy;
import org.eontechnology.and.jsonrpc.JsonException;
import org.eontechnology.and.peer.core.common.exceptions.RemotePeerException;

/** Proxy for remote peer services */
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
    try {
      return proxy.post(serviceName + "." + method, params, clazz);
    } catch (JsonException e) {
      throw new RemotePeerException(e);
    }
  }
}
