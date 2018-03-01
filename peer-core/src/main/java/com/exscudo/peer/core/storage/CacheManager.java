package com.exscudo.peer.core.storage;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.j256.ormlite.support.ConnectionSource;

public class CacheManager {
    private volatile static Map<ConnectionSourceClass, Object> cacheMap = null;

    public synchronized static <TCache> TCache createCache(ConnectionSource connectionSource, Class<TCache> clazz) {
        Objects.requireNonNull(connectionSource);
        Objects.requireNonNull(clazz);

        ConnectionSourceClass clazzKey = new ConnectionSourceClass(connectionSource, clazz);
        Object cacheObj = lookupCache(clazzKey);
        if (cacheObj == null) {
            try {
                cacheObj = clazz.getConstructor().newInstance();
                cacheMap.put(clazzKey, cacheObj);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        TCache castObj = clazz.cast(cacheObj);
        return castObj;
    }

    public synchronized static <TCache> TCache lookupCache(ConnectionSource connectionSource, Class<TCache> clazz) {
        Objects.requireNonNull(connectionSource);

        Object cacheObj = lookupCache(new ConnectionSourceClass(connectionSource, clazz));
        if (cacheObj == null) {
            return null;
        }
        TCache castObj = clazz.cast(cacheObj);
        return castObj;
    }

    private synchronized static Object lookupCache(ConnectionSourceClass classKey) {
        if (cacheMap == null) {
            cacheMap = new HashMap<>();
        }
        Object o = cacheMap.get(classKey);
        if (o == null) {
            return null;
        } else {
            return o;
        }
    }

    private static class ConnectionSourceClass {
        private final ConnectionSource connectionSource;
        private final Class<?> clazz;

        private ConnectionSourceClass(ConnectionSource connectionSource, Class<?> clazz) {
            this.connectionSource = connectionSource;
            this.clazz = clazz;
        }

        @Override
        public int hashCode() {
            int base = 31;
            int r = base + clazz.hashCode();
            r = base * r + connectionSource.hashCode();
            return r;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            ConnectionSourceClass other = (ConnectionSourceClass) obj;
            if (!clazz.equals(other.clazz)) {
                return false;
            } else if (!connectionSource.equals(other.connectionSource)) {
                return false;
            }

            return true;
        }
    }
}
