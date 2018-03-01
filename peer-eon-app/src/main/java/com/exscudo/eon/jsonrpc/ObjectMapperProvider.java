package com.exscudo.eon.jsonrpc;

import com.exscudo.eon.jsonrpc.serialization.*;
import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * JSON custom serialisation configurator
 */
public class ObjectMapperProvider {

    public static ObjectMapper createMapper() {

        final ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        mapper.registerModule(createModule());

        return mapper;
    }

    public static Module createModule() {

        SimpleModule module = new SimpleModule();
        module.addSerializer(Transaction.class, new TransactionSerializer());
        module.addDeserializer(Transaction.class, new TransactionDeserializer());
        module.addSerializer(Difficulty.class, new DifficultySerializer());
        module.addDeserializer(Difficulty.class, new DifficultyDeserializer());
        module.addSerializer(Block.class, new BlockSerializer());
        module.addDeserializer(Block.class, new BlockDeserializer());

        return module;
    }
}
