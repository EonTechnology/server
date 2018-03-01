package com.exscudo.peer.core.crypto;

import java.util.Map;

/**
 * Convert object of supported types to Map
 */
public interface ISignedObjectMapper {

    /**
     * Converts object to Map.
     *
     * @param object object to convert
     * @return Map
     */
    Map<String, Object> convert(Object object);
}
