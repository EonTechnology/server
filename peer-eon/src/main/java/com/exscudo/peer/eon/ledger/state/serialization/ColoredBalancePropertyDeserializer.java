package com.exscudo.peer.eon.ledger.state.serialization;

import java.io.IOException;

import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.eon.PropertyType;
import com.exscudo.peer.eon.ledger.AccountPropertyDeserializer;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;

public class ColoredBalancePropertyDeserializer extends AccountPropertyDeserializer {

    @Override
    public Object deserialize(Account account) throws IOException {

        AccountProperty p = account.getProperty(PropertyType.COLORED_BALANCE);
        if (p == null) {
            return new ColoredBalanceProperty();
        }

        ColoredBalanceProperty coloredBalance = new ColoredBalanceProperty(p.getData());

        return coloredBalance;
    }
}
