package org.eontechology.and.jsonrpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

class InnerServiceMethod {
    private final Object innerService;
    private final Method method;

    InnerServiceMethod(Object innerService, Method method) {
        this.innerService = Objects.requireNonNull(innerService);
        this.method = Objects.requireNonNull(method);
    }

    public Object invoke(Object... args) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(innerService, args);
    }

    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }

    public Class<?> getServiceClass() {
        return innerService.getClass();
    }
}
