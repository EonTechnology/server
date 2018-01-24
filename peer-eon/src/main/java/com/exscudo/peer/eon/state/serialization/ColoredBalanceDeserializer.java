package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.eon.AccountPropertyDeserializer;
import com.exscudo.peer.eon.state.ColoredBalance;
import com.exscudo.peer.eon.utils.ColoredCoinId;

public class ColoredBalanceDeserializer extends AccountPropertyDeserializer {

	@Override
	public Object deserialize(IAccount account) throws IOException {

		AccountProperty p = account.getProperty(PropertyType.COLORED_BALANCE);
		if (p == null) {
			return null;
		}

		ColoredBalance coloredBalance = new ColoredBalance();
		try {
			for (Map.Entry<String, Object> e : p.getData().entrySet()) {

				long color = ColoredCoinId.convert(String.valueOf(e.getKey()));
				long balance = Long.parseLong(String.valueOf(e.getValue()));

				coloredBalance.setBalance(balance, color);

			}
		} catch (NumberFormatException e) {
			throw new IOException(e);
		}

		return coloredBalance;
	}

}
