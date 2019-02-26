package org.eontechology.and.eon.app.jsonrpc.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.eontechology.and.eon.app.utils.mapper.TransportTransactionMapper;
import org.eontechology.and.peer.core.data.Transaction;

/**
 * JSON custom serialisation of {@code Transaction}
 *
 * @see Transaction
 */
public class TransactionSerializer extends StdSerializer<Transaction> {
    private static final long serialVersionUID = -7988862783097982697L;

    public TransactionSerializer() {
        super(Transaction.class);
    }

    @Override
    public void serialize(Transaction value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeObject(TransportTransactionMapper.convert(value));
    }
}
