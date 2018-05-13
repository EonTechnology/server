package com.exscudo.eon.app.jsonrpc;

import com.exscudo.eon.app.jsonrpc.serialization.AccountDeserializer;
import com.exscudo.eon.app.jsonrpc.serialization.AccountSerializer;
import com.exscudo.eon.app.jsonrpc.serialization.BlockDeserializer;
import com.exscudo.eon.app.jsonrpc.serialization.BlockSerializer;
import com.exscudo.eon.app.jsonrpc.serialization.DifficultyDeserializer;
import com.exscudo.eon.app.jsonrpc.serialization.DifficultySerializer;
import com.exscudo.eon.app.jsonrpc.serialization.TransactionDeserializer;
import com.exscudo.eon.app.jsonrpc.serialization.TransactionSerializer;
import com.exscudo.peer.core.api.Difficulty;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
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
        module.addSerializer(Account.class, new AccountSerializer());
        module.addDeserializer(Account.class, new AccountDeserializer());

        return module;
    }
}
