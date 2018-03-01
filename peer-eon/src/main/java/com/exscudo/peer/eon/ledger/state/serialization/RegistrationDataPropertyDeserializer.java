package com.exscudo.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.Objects;

import com.exscudo.peer.core.PropertyType;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.eon.ledger.AccountPropertyDeserializer;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;

public class RegistrationDataPropertyDeserializer extends AccountPropertyDeserializer {

    @Override
    public Object deserialize(Account account) throws IOException {

        AccountProperty p = account.getProperty(PropertyType.REGISTRATION);
        Objects.requireNonNull(p, "Public key has not been initialized.");

        try {
            Object o = p.getData().get("publicKey");
            byte[] publicKey = Format.convert(String.valueOf(o));
            return new RegistrationDataProperty(publicKey);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
