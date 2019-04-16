package org.eontechnology.and.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.HashMap;

import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.AccountProperty;
import org.eontechnology.and.peer.eon.PropertyType;
import org.eontechnology.and.peer.eon.ledger.AccountPropertySerializer;
import org.eontechnology.and.peer.eon.ledger.state.RegistrationDataProperty;

public class RegistrationDataPropertySerializer extends AccountPropertySerializer<RegistrationDataProperty> {

    public RegistrationDataPropertySerializer() {
        super(RegistrationDataProperty.class);
    }

    @Override
    public Account doSerialize(RegistrationDataProperty registrationData, Account account) throws IOException {
        HashMap<String, Object> data = new HashMap<>();
        data.put("pk", Format.convert(registrationData.getPublicKey()));

        return account.putProperty(new AccountProperty(PropertyType.REGISTRATION, data));
    }
}
