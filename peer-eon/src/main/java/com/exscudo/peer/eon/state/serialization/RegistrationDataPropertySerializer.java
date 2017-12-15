package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.HashMap;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.AccountPropertySerializer;
import com.exscudo.peer.eon.state.RegistrationData;

public class RegistrationDataPropertySerializer extends AccountPropertySerializer<RegistrationData> {

	public RegistrationDataPropertySerializer() {
		super(RegistrationData.class);
	}

	@Override
	public void doSerialize(RegistrationData registrationData, IAccount account) throws IOException {
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("publicKey", Format.convert(registrationData.getPublicKey()));

		account.putProperty(new AccountProperty(PropertyType.REGISTRATION, data));
	}

}
