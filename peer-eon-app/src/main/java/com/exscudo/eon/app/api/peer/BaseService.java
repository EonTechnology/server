package com.exscudo.eon.app.api.peer;

import com.exscudo.jsonrpc.RequestContextHolder;

public class BaseService {

    public String getRemoteHost() {
        return RequestContextHolder.getRemoteHost();
    }
}
