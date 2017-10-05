package com.exscudo.eon.jsonrpc.serialization;

import java.io.IOException;

import com.exscudo.peer.core.data.Difficulty;
import com.exscudo.peer.core.utils.Format;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * JSON custom serialisation of {@code Difficulty}
 *
 * @see Difficulty
 */
public class DifficultySerializer extends StdSerializer<Difficulty> {
	private static final long serialVersionUID = -1290081864260004542L;

	public DifficultySerializer() {
		super(Difficulty.class);
	}

	@Override
	public void serialize(Difficulty value, JsonGenerator gen, SerializerProvider provider) throws IOException {

		gen.writeStartObject();
		gen.writeStringField(StringConstant.cumulativeDifficulty, value.getDifficulty().toString());
		gen.writeStringField(StringConstant.lastBlockID, Format.ID.blockId(value.getLastBlockID()));
		gen.writeEndObject();

	}

}
