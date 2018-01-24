package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.eon.AccountPropertyDeserializer;
import com.exscudo.peer.eon.state.ColoredCoin;

public class ColoredCoinDeserializer extends AccountPropertyDeserializer {

	@Override
	public Object deserialize(IAccount account) throws IOException {

		AccountProperty p = account.getProperty(PropertyType.COLORED_COIN);
		if (p == null) {
			return null;
		}

		ColoredCoin coloredCoin = new ColoredCoin();

		Map<String, Object> map = p.getData();
		try {

			long moneySupply = Long.parseLong(String.valueOf(map.get("moneySupply")));
			int decimalPoint = Integer.parseInt(String.valueOf(map.get("decimalPoint")));
			// Sets only on money creation
			int timestamp = Integer.parseInt(String.valueOf(map.get("timestamp")));

			coloredCoin.setMoneySupply(moneySupply);
			coloredCoin.setDecimalPoint(decimalPoint);
			coloredCoin.setTimestamp(timestamp);

		} catch (NumberFormatException e) {
			throw new IOException(e);
		}

		return coloredCoin;

	}

}
