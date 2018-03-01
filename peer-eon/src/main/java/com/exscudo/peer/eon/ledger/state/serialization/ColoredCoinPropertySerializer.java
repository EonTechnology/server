package com.exscudo.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.HashMap;

import com.exscudo.peer.core.PropertyType;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.eon.ledger.AccountPropertySerializer;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;

public class ColoredCoinPropertySerializer extends AccountPropertySerializer<ColoredCoinProperty> {

    public ColoredCoinPropertySerializer() {
        super(ColoredCoinProperty.class);
    }

    @Override
    public Account doSerialize(ColoredCoinProperty coloredCoin, Account account) throws IOException {

        if (coloredCoin.getMoneySupply() != 0) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("moneySupply", coloredCoin.getMoneySupply());
            data.put("decimalPoint", coloredCoin.getDecimalPoint());
            // Sets only on money creation
            data.put("timestamp", coloredCoin.getTimestamp());

            return account.putProperty(new AccountProperty(PropertyType.COLORED_COIN, data));
        } else {
            return account.removeProperty(PropertyType.COLORED_COIN);
        }
    }
}
