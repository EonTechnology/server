package org.eontechology.and.eon.app.jsonrpc;

import java.util.Map;

import org.eontechology.and.jsonrpc.CompositeInnerService;
import org.eontechology.and.jsonrpc.InnerService;
import org.eontechology.and.jsonrpc.JrpcService;

public class EonService extends JrpcService {

    public EonService(Object obj) {
        super(new InnerService(obj), ObjectMapperProvider.createModule());
    }

    public EonService(Map<String, Object> map) {
        super(new CompositeInnerService(map), ObjectMapperProvider.createModule());
    }
}
