package com.exscudo.jsonrpc;

public class JsonBaseService {

    private ThreadLocal<String> remoteHost = new ThreadLocal<>();

    public String getRemoteHost() {
        return remoteHost.get();
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost.set(remoteHost);
    }
}
