package com.exscudo.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.PropertyType;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.eon.ledger.AccountPropertyDeserializer;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;

public class ColoredCoinPropertyDeserializer extends AccountPropertyDeserializer {

    @Override
    public Object deserialize(Account account) throws IOException {

        AccountProperty p = account.getProperty(PropertyType.COLORED_COIN);
        if (p == null) {
            return new ColoredCoinProperty();
        }

        ColoredCoinProperty coloredCoin = new ColoredCoinProperty();

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
