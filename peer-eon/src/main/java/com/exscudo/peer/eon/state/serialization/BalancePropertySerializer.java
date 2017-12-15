package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.HashMap;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.eon.AccountPropertySerializer;
import com.exscudo.peer.eon.state.Balance;

public class BalancePropertySerializer extends AccountPropertySerializer<Balance> {

	public BalancePropertySerializer() {
		super(Balance.class);
	}

	@Override
	public void doSerialize(Balance balance, IAccount account) throws IOException {

		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("amount", balance.getValue());

		account.putProperty(new AccountProperty(PropertyType.BALANCE, data));

	}

}
