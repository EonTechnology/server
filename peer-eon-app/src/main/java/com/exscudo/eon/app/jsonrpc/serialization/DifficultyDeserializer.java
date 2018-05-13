package com.exscudo.eon.app.jsonrpc.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.eon.app.utils.mapper.DifficultyMapper;
import com.exscudo.peer.core.api.Difficulty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * JSON custom deserialization of {@code Difficulty}
 *
 * @see Difficulty
 */
public class DifficultyDeserializer extends StdDeserializer<Difficulty> {
    private static final long serialVersionUID = -6968261230877059237L;

    public DifficultyDeserializer() {
        super(Difficulty.class);
    }

    @Override
    public Difficulty deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        try {

            Map<String, Object> map = p.readValueAs(new TypeReference<Map<String, Object>>() {
            });

            return DifficultyMapper.convert(map);
        } catch (IllegalArgumentException ignored) {
        }

        return null;
    }
}
