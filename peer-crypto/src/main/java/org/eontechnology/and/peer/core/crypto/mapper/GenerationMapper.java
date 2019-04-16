package org.eontechnology.and.peer.core.crypto.mapper;

import java.util.HashMap;
import java.util.Map;

import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.data.Generation;
import org.eontechnology.and.peer.core.data.identifier.BlockID;

class GenerationMapper {

    public static Map<String, Object> convert(Generation generation, BlockID networkID) {

        Map<String, Object> map = new HashMap<>();

        map.put(Constants.GENERATION_SIGNATURE, Format.convert(generation.prevSignature));
        map.put(Constants.NETWORK, networkID.toString());

        return map;
    }
}
