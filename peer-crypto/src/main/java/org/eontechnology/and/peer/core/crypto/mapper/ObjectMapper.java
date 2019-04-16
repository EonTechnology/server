package org.eontechnology.and.peer.core.crypto.mapper;

import java.util.Map;

import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Generation;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.BlockID;

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
            return CryptoTransactionMapper.convert((Transaction) object, networkID);
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
