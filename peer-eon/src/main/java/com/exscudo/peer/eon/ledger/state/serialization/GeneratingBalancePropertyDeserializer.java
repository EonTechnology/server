package com.exscudo.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.Map;

import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.eon.PropertyType;
import com.exscudo.peer.eon.ledger.AccountPropertyDeserializer;
import com.exscudo.peer.eon.ledger.state.GeneratingBalanceProperty;

public class GeneratingBalancePropertyDeserializer extends AccountPropertyDeserializer {

    @Override
    public Object deserialize(Account account) throws IOException {

        AccountProperty p = account.getProperty(PropertyType.DEPOSIT);
        if (p == null) {
            return new GeneratingBalanceProperty();
        }

        Map<String, Object> map = p.getData();
        try {

            long balance = Long.parseLong(String.valueOf(map.get("amount")));
            int timestamp = Integer.parseInt(String.valueOf(map.get("timestamp")));

            GeneratingBalanceProperty generatingBalance = new GeneratingBalanceProperty();
            generatingBalance.setTimestamp(timestamp);
            generatingBalance.setValue(balance);
            return generatingBalance;
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
    }
}
