package com.exscudo.eon.app.jsonrpc.serialization;

import java.io.IOException;

import com.exscudo.eon.app.utils.mapper.TransportBlockMapper;
import com.exscudo.peer.core.data.Block;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * JSON custom serialisation of {@code Block}
 *
 * @see Block
 */
public class BlockSerializer extends StdSerializer<Block> {
    private static final long serialVersionUID = -1693121409934138590L;

    public BlockSerializer() {
        super(Block.class);
    }

    @Override
    public void serialize(Block value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeObject(TransportBlockMapper.convert(value));
    }
}
