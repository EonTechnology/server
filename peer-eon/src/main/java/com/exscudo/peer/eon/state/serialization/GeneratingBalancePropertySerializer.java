package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.HashMap;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.eon.AccountPropertySerializer;
import com.exscudo.peer.eon.state.GeneratingBalance;

public class GeneratingBalancePropertySerializer extends AccountPropertySerializer<GeneratingBalance> {

	public GeneratingBalancePropertySerializer() {
		super(GeneratingBalance.class);
	}

	@Override
	public void doSerialize(GeneratingBalance generatingBalance, IAccount account) throws IOException {

		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("amount", generatingBalance.getValue());
		data.put("height", generatingBalance.getHeight());

		account.putProperty(new AccountProperty(PropertyType.DEPOSIT, data));

	}
}
