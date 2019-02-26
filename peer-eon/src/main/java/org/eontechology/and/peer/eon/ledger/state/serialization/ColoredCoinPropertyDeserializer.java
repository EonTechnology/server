package org.eontechology.and.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.Map;

import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.AccountProperty;
import org.eontechology.and.peer.eon.PropertyType;
import org.eontechology.and.peer.eon.ledger.AccountPropertyDeserializer;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechology.and.peer.eon.ledger.state.ColoredCoinProperty;

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

            long moneySupply = Long.parseLong(String.valueOf(map.get("supply")));
            int decimalPoint = Integer.parseInt(String.valueOf(map.get("decimal")));
            // Sets only on money creation
            int timestamp = Integer.parseInt(String.valueOf(map.get("timestamp")));

            ColoredCoinEmitMode emitMode = ColoredCoinEmitMode.PRESET;
            String mode = (String) map.get("mode");
            if (mode != null) {
                emitMode = ColoredCoinEmitMode.fromString(mode);
                if (emitMode == null) {
                    throw new UnsupportedOperationException();
                }
            }

            coloredCoin.setAttributes(new ColoredCoinProperty.Attributes(decimalPoint, timestamp));
            coloredCoin.setMoneySupply(moneySupply);
            coloredCoin.setEmitMode(emitMode);
        } catch (NumberFormatException | UnsupportedOperationException e) {
            throw new IOException(e);
        }

        return coloredCoin;
    }
}
