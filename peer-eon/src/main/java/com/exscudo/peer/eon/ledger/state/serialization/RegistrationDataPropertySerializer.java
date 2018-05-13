package com.exscudo.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.HashMap;

import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.eon.PropertyType;
import com.exscudo.peer.eon.ledger.AccountPropertySerializer;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;

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
