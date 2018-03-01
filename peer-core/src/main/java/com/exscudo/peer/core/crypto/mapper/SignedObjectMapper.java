package com.exscudo.peer.core.crypto.mapper;

import java.util.Map;

import com.exscudo.peer.core.crypto.ISignedObjectMapper;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.BlockID;

/**
 * Convert object of supported types to Map
 */
public class SignedObjectMapper implements ISignedObjectMapper {

    private final BlockID networkID;

    public SignedObjectMapper(BlockID networkID) {
        this.networkID = networkID;
    }

    /**
     * Converts object to Map.
     *
     * @param object object to convert
     * @return bytes
     */
    @Override
    public Map<String, Object> convert(Object object) {

        if (object instanceof Transaction) {
            Map<String, Object> map = TransactionMapper.convert((Transaction) object);
            map.put("network", networkID.toString());
            return map;
        }
        if (object instanceof Block) {
            return BlockMapper.convert((Block) object);
        }

        throw new UnsupportedOperationException("Unsupported object type : [" + object.getClass() + "]");
    }
}
