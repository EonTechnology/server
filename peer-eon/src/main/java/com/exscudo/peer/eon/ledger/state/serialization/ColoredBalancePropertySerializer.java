package com.exscudo.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.PropertyType;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.eon.ledger.AccountPropertySerializer;
import com.exscudo.peer.eon.ColoredCoinID;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;

public class ColoredBalancePropertySerializer extends AccountPropertySerializer<ColoredBalanceProperty> {

    public ColoredBalancePropertySerializer() {
        super(ColoredBalanceProperty.class);
    }

    @Override
    public Account doSerialize(ColoredBalanceProperty coloredBalance, Account account) throws IOException {

        HashMap<String, Object> balances = new HashMap<>();
        for (Map.Entry<ColoredCoinID, Long> e : coloredBalance.balancesEntrySet()) {
            long balance = e.getValue();
            if (balance != 0) {
                balances.put(e.getKey().toString(), balance);
            }
        }

        if (balances.isEmpty()) {
            return account.removeProperty(PropertyType.COLORED_BALANCE);
        } else {
            return account.putProperty(new AccountProperty(PropertyType.COLORED_BALANCE, balances));
        }
    }
}
