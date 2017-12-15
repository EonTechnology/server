package com.exscudo.eon.jsonrpc.serialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.mapper.transport.BlockMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

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
	public Block deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

		try {

			ObjectMapper mapper = (ObjectMapper) p.getCodec();

			JsonNode node = mapper.readTree(p);

			Map<?, ?> map = mapper.convertValue(node, Map.class);

			Map<String, Object> data = new HashMap<>();
			for (Object obj_Entry : map.entrySet()) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj_Entry;
				data.put(entry.getKey().toString(), entry.getValue());
			}

			return BlockMapper.convert(data);

		} catch (IllegalArgumentException e) {
			throw new IOException(e);
		}

	}
}
