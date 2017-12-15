package com.exscudo.eon.jsonrpc.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.mapper.transport.BlockMapper;
import com.exscudo.peer.core.utils.Format;
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

		Map<String, Object> map = BlockMapper.convert(value);
		map.put(StringConstant.id, Format.ID.blockId(value.getID()));
		gen.writeObject(map);

	}
}
