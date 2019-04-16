package org.eontechnology.and.jsonrpc;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class InnerService implements IInnerService {
    private final Map<String, Method> methods = new HashMap<String, Method>();
    private final Object innerService;

    public InnerService(Object innerService) {
        this.innerService = innerService;
    }

    @Override
    public InnerServiceMethod getMethod(String methodName) {
        Method method = null;
        if (methods.containsKey(methodName)) {
            method = methods.get(methodName);
        }
        if (method == null) {
            method = MethodUtils.getMethod(innerService, methodName);
            if (method == null) {
                return null;
            }
            methods.put(methodName, method);
        }

        return new InnerServiceMethod(innerService, method);
    }
}
