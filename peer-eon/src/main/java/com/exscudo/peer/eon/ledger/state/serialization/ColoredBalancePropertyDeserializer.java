package com.exscudo.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.PropertyType;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.eon.ledger.AccountPropertyDeserializer;
import com.exscudo.peer.eon.ColoredCoinID;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;

public class ColoredBalancePropertyDeserializer extends AccountPropertyDeserializer {

    @Override
    public Object deserialize(Account account) throws IOException {

        AccountProperty p = account.getProperty(PropertyType.COLORED_BALANCE);
        if (p == null) {
            return new ColoredBalanceProperty();
        }

        ColoredBalanceProperty coloredBalance = new ColoredBalanceProperty();
        try {
            for (Map.Entry<String, Object> e : p.getData().entrySet()) {

                ColoredCoinID color = new ColoredCoinID(String.valueOf(e.getKey()));
                long balance = Long.parseLong(String.valueOf(e.getValue()));

                coloredBalance.setBalance(balance, color);
            }
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }

        return coloredBalance;
    }
}
