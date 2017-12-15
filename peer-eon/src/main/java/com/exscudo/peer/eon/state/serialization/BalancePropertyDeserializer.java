package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.eon.AccountPropertyDeserializer;
import com.exscudo.peer.eon.state.Balance;

public class BalancePropertyDeserializer extends AccountPropertyDeserializer {

	@Override
	public Object deserialize(IAccount account) throws IOException {

		AccountProperty p = account.getProperty(PropertyType.BALANCE);
		if (p == null) {
			return null;
		}

		Map<String, Object> map = p.getData();
		try {
			long balance = Long.parseLong(String.valueOf(map.get("amount")));
			return new Balance(balance);
		} catch (NumberFormatException e) {
			throw new IOException(e);
		}

	}

}
