package com.exscudo.peer.eon.stubs;

public class BaseService {

	private ThreadLocal<String> remoteHost = new ThreadLocal<>();

	public String getRemoteHost() {
		return remoteHost.get();
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost.set(remoteHost);
	}
}
