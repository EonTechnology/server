package com.exscudo.peer.core.crypto.mapper;

import java.util.Map;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Generation;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;

/**
 * Convert object of supported types to Map
 */
public class ObjectMapper {
    private final BlockID networkID;

    public ObjectMapper(BlockID networkID) {

        this.networkID = networkID;
    }

    /**
     * Converts object to Map.
     *
     * @param object object to convert
     * @return bytes
     */
    public Map<String, Object> convert(Object object) {

        if (object instanceof Transaction) {
            return TransactionMapper.convert((Transaction) object, networkID);
        }
        if (object instanceof Block) {
            return BlockMapper.convert((Block) object, networkID);
        }
        if (object instanceof Generation) {
            return GenerationMapper.convert((Generation) object, networkID);
        }

        throw new UnsupportedOperationException("Unsupported object type : [" + object.getClass() + "]");
    }
}
