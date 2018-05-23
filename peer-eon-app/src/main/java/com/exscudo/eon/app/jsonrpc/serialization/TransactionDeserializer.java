package com.exscudo.eon.app.jsonrpc.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.eon.app.utils.mapper.TransaportTransactionMapper;
import com.exscudo.peer.core.data.Transaction;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * JSON custom deserialization of {@code Transaction}
 *
 * @see Transaction
 */
public class TransactionDeserializer extends StdDeserializer<Transaction> {
    private static final long serialVersionUID = 416600393120217440L;

    public TransactionDeserializer() {
        super(Transaction.class);
    }

    @Override
    public Transaction deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        try {

            Map<String, Object> map = p.readValueAs(new TypeReference<Map<String, Object>>() {
            });
            return TransaportTransactionMapper.convert(map);
        } catch (IllegalArgumentException ignored) {
        }

        return null;
    }
}
