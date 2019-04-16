package org.eontechnology.and.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.HashMap;

import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.AccountProperty;
import org.eontechnology.and.peer.eon.PropertyType;
import org.eontechnology.and.peer.eon.ledger.AccountPropertySerializer;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinProperty;

public class ColoredCoinPropertySerializer extends AccountPropertySerializer<ColoredCoinProperty> {

    public ColoredCoinPropertySerializer() {
        super(ColoredCoinProperty.class);
    }

    @Override
    public Account doSerialize(ColoredCoinProperty coloredCoin, Account account) throws IOException {

        if (coloredCoin.getAttributes() != null) {

            HashMap<String, Object> data = new HashMap<>();

            data.put("supply", coloredCoin.getMoneySupply());

            ColoredCoinProperty.Attributes attributes = coloredCoin.getAttributes();
            data.put("decimal", attributes.decimalPoint);
            // Sets only on money creation
            data.put("timestamp", attributes.timestamp);

            ColoredCoinEmitMode emitMode = coloredCoin.getEmitMode();
            if (emitMode != ColoredCoinEmitMode.PRESET) {
                data.put("mode", emitMode.toString());
            }

            return account.putProperty(new AccountProperty(PropertyType.COLORED_COIN, data));
        } else {
            return account.removeProperty(PropertyType.COLORED_COIN);
        }
    }
}
