package com.exscudo.eon.app.jsonrpc.serialization;

import java.io.IOException;

import com.exscudo.eon.app.utils.mapper.AccountMapper;
import com.exscudo.peer.core.data.Account;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class AccountSerializer extends StdSerializer<Account> {
    public AccountSerializer() {
        super(Account.class);
    }

    @Override
    public void serialize(Account value, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        gen.writeObject(AccountMapper.convert(value));
    }
}
