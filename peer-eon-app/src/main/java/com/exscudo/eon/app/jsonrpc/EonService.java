package com.exscudo.eon.app.jsonrpc;

import java.util.Map;

import com.exscudo.jsonrpc.JrpcService;

public class EonService extends JrpcService {
    public EonService(Map<String, Object> innerServices) {
        super(innerServices, ObjectMapperProvider.createModule());
    }
}
