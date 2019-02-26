package org.eontechology.and.eon.app.jsonrpc.serialization;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.eontechology.and.eon.app.utils.mapper.TransportBlockMapper;
import org.eontechology.and.peer.core.data.Block;

/**
 * JSON custom deserialization of {@code Block}
 *
 * @see Block
 */
public class BlockDeserializer extends StdDeserializer<Block> {
    private static final long serialVersionUID = 7107449742243526806L;

    public BlockDeserializer() {
        super(Block.class);
    }

    @Override
    public Block deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        try {

            Map<String, Object> map = p.readValueAs(new TypeReference<Map<String, Object>>() {
            });
            return TransportBlockMapper.convert(map);
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        }
    }
}
