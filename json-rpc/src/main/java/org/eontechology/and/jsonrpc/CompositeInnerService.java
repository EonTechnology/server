package org.eontechology.and.jsonrpc;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CompositeInnerService implements IInnerService {
    private final Map<String, Method> methods = new HashMap<String, Method>();
    private final Map<String, Object> innerServices;

    public CompositeInnerService(Map<String, Object> innerServices) {
        this.innerServices = innerServices;
    }

    @Override
    public InnerServiceMethod getMethod(String methodName) {

        String[] split = methodName.split("\\.");
        Object innerService = innerServices.get(split[0]);

        if (innerService != null) {

            Method method = null;
            if (methods.containsKey(methodName)) {
                method = methods.get(methodName);
            }
            if (method == null) {
                method = MethodUtils.getMethod(innerService, split[1]);
                if (method == null) {
                    return null;
                }
                methods.put(methodName, method);
            }
            return new InnerServiceMethod(innerService, method);
        }

        return null;
    }
}
