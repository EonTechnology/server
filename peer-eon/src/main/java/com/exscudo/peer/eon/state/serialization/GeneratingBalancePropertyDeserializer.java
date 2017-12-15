package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.eon.AccountPropertyDeserializer;
import com.exscudo.peer.eon.state.GeneratingBalance;

public class GeneratingBalancePropertyDeserializer extends AccountPropertyDeserializer {

	@Override
	public Object deserialize(IAccount account) throws IOException {

		AccountProperty p = account.getProperty(PropertyType.DEPOSIT);
		if (p == null) {
			return null;
		}

		Map<String, Object> map = p.getData();
		try {

			long balance = Long.parseLong(String.valueOf(map.get("amount")));
			int height = Integer.parseInt(String.valueOf(map.get("height")));

			GeneratingBalance generatingBalance = new GeneratingBalance();
			generatingBalance.setHeight(height);
			generatingBalance.setValue(balance);
			return generatingBalance;

		} catch (NumberFormatException e) {
			throw new IOException(e);
		}

	}

}
