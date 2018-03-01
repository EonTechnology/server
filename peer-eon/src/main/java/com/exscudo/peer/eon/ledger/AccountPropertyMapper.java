package com.exscudo.peer.eon.ledger;

import java.util.HashMap;
import java.util.Objects;

/**
 * List of known serializers and deserializers for properties.
 */
public class AccountPropertyMapper {
    protected HashMap<Class<?>, AccountPropertySerializer<?>> serializersMap = null;
    protected HashMap<Class<?>, AccountPropertyDeserializer> deserializersMap = null;

    public AccountPropertyMapper addSerializer(AccountPropertySerializer<?> serializer) {
        Objects.requireNonNull(serializer);
        Class<?> clazz = serializer.handledType();
        if (clazz == null) {
            throw new IllegalArgumentException();
        }
        if (serializersMap == null) {
            serializersMap = new HashMap<>();
        }
        serializersMap.put(clazz, serializer);
        return this;
    }

    public AccountPropertySerializer<?> findSerializer(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        if (serializersMap == null) {
            return null;
        }
        return serializersMap.get(clazz);
    }

    public AccountPropertyMapper addDeserializer(Class<?> clazz, AccountPropertyDeserializer deserializer) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(deserializer);

        if (deserializersMap == null) {
            deserializersMap = new HashMap<>();
        }
        deserializersMap.put(clazz, deserializer);
        return this;
    }

    public AccountPropertyDeserializer findDeserializer(Class<?> clazz) {
        Objects.requireNonNull(clazz);

        if (deserializersMap == null) {
            return null;
        }
        return deserializersMap.get(clazz);
    }
}
