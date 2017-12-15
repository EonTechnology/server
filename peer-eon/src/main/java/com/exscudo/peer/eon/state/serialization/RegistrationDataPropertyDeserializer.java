package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.Objects;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.AccountPropertyDeserializer;
import com.exscudo.peer.eon.state.RegistrationData;

public class RegistrationDataPropertyDeserializer extends AccountPropertyDeserializer {

	@Override
	public Object deserialize(IAccount account) throws IOException {

		AccountProperty p = account.getProperty(PropertyType.REGISTRATION);
		Objects.requireNonNull(p, "Public key has not been initialized.");

		try {
			Object o = p.getData().get("publicKey");
			byte[] publicKey = Format.convert(String.valueOf(o));
			return new RegistrationData(publicKey);
		} catch (Exception e) {
			throw new IOException(e);
		}

	}
}
