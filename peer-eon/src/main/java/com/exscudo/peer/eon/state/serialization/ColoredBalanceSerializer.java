package com.exscudo.peer.eon.state.serialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.eon.AccountPropertySerializer;
import com.exscudo.peer.eon.state.ColoredBalance;
import com.exscudo.peer.eon.utils.ColoredCoinId;

public class ColoredBalanceSerializer extends AccountPropertySerializer<ColoredBalance> {

	public ColoredBalanceSerializer() {
		super(ColoredBalance.class);
	}

	@Override
	public void doSerialize(ColoredBalance coloredBalance, IAccount account) throws IOException {

		HashMap<String, Object> balances = new HashMap<>();
		for (Map.Entry<Long, Long> e : coloredBalance.balancesEntrySet()) {
			long balance = e.getValue();
			if (balance != 0) {
				balances.put(ColoredCoinId.convert(e.getKey()), balance);
			}
		}

		if (balances.isEmpty()) {
			account.removeProperty(PropertyType.COLORED_BALANCE);
		} else {
			account.putProperty(new AccountProperty(PropertyType.COLORED_BALANCE, balances));
		}

	}
}
