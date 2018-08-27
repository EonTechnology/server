package com.exscudo.eon.app.jsonrpc;

import java.util.Map;

import com.exscudo.jsonrpc.CompositeInnerService;
import com.exscudo.jsonrpc.InnerService;
import com.exscudo.jsonrpc.JrpcService;

public class EonService extends JrpcService {

    public EonService(Object obj) {
        super(new InnerService(obj), ObjectMapperProvider.createModule());
    }

    public EonService(Map<String, Object> map) {
        super(new CompositeInnerService(map), ObjectMapperProvider.createModule());
    }
}
