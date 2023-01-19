package org.eontechnology.and.jsonrpc;

public class RequestContextHolder {

  private static final ThreadLocal<String> remoteHost = new ThreadLocal<>();

  public static String getRemoteHost() {
    return remoteHost.get();
  }

  public static void setRemoteHost(String host) {
    remoteHost.set(host);
  }

  public static void resetRemoteHost() {
    remoteHost.remove();
  }
}
