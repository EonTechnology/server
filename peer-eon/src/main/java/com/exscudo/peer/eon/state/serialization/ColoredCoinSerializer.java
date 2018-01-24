package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.HashMap;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.eon.AccountPropertySerializer;
import com.exscudo.peer.eon.state.ColoredCoin;

public class ColoredCoinSerializer extends AccountPropertySerializer<ColoredCoin> {

	public ColoredCoinSerializer() {
		super(ColoredCoin.class);
	}

	@Override
	public void doSerialize(ColoredCoin coloredCoin, IAccount account) throws IOException {

		if (coloredCoin.getMoneySupply() != 0) {
			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put("moneySupply", coloredCoin.getMoneySupply());
			data.put("decimalPoint", coloredCoin.getDecimalPoint());
			// Sets only on money creation
			data.put("timestamp", coloredCoin.getTimestamp());

			account.putProperty(new AccountProperty(PropertyType.COLORED_COIN, data));
		} else {
			account.removeProperty(PropertyType.COLORED_COIN);
		}

	}

}
