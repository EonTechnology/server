package com.exscudo.eon.app.jsonrpc.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.eon.app.utils.mapper.AccountMapper;
import com.exscudo.peer.core.data.Account;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class AccountDeserializer extends StdDeserializer<Account> {
    public AccountDeserializer() {
        super(Account.class);
    }

    @Override
    public Account deserialize(JsonParser jsonParser,
                               DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        Map<String, Object> map = jsonParser.readValueAs(new TypeReference<Map<String, Object>>() {
        });

        try {
            return AccountMapper.convert(map);
        } catch (IllegalArgumentException ignored) {

        }
        return null;
    }
}
