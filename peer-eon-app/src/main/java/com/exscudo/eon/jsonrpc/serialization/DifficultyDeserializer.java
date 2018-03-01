package com.exscudo.eon.jsonrpc.serialization;

import java.io.IOException;
import java.math.BigInteger;

import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        BlockID lastBlockID = null;
        try {
            lastBlockID = new BlockID(node.get(StringConstant.lastBlockID).asText());
        } catch (Exception ignore) {
        }
        BigInteger difficulty = new BigInteger(node.get(StringConstant.cumulativeDifficulty).asText());

        return new Difficulty(lastBlockID, difficulty);
    }
}
