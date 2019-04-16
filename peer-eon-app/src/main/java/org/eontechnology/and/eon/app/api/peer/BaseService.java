package org.eontechnology.and.eon.app.api.peer;

import org.eontechnology.and.jsonrpc.RequestContextHolder;

public class BaseService {

    public String getRemoteHost() {
        return RequestContextHolder.getRemoteHost();
    }
}
