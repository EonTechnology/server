package org.eontechnology.and.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.Objects;

import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.AccountProperty;
import org.eontechnology.and.peer.eon.PropertyType;
import org.eontechnology.and.peer.eon.ledger.AccountPropertyDeserializer;
import org.eontechnology.and.peer.eon.ledger.state.RegistrationDataProperty;

public class RegistrationDataPropertyDeserializer extends AccountPropertyDeserializer {

    @Override
    public Object deserialize(Account account) throws IOException {

        AccountProperty p = account.getProperty(PropertyType.REGISTRATION);
        Objects.requireNonNull(p, "Public key has not been initialized.");

        try {
            Object o = p.getData().get("pk");
            byte[] publicKey = Format.convert(String.valueOf(o));
            return new RegistrationDataProperty(publicKey);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
