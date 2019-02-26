package org.eontechology.and.eon.app.api.peer;

import org.eontechology.and.jsonrpc.RequestContextHolder;

public class BaseService {

    public String getRemoteHost() {
        return RequestContextHolder.getRemoteHost();
    }
}
