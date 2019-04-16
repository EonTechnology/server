package org.eontechnology.and.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.AccountProperty;
import org.eontechnology.and.peer.eon.PropertyType;
import org.eontechnology.and.peer.eon.ledger.AccountPropertySerializer;
import org.eontechnology.and.peer.eon.ledger.state.ColoredBalanceProperty;

public class ColoredBalancePropertySerializer extends AccountPropertySerializer<ColoredBalanceProperty> {

    public ColoredBalancePropertySerializer() {
        super(ColoredBalanceProperty.class);
    }

    @Override
    public Account doSerialize(ColoredBalanceProperty coloredBalance, Account account) throws IOException {

        Map<String, Object> balances = new TreeMap<>(coloredBalance.getProperty());

        if (balances.isEmpty()) {
            return account.removeProperty(PropertyType.COLORED_BALANCE);
        } else {
            return account.putProperty(new AccountProperty(PropertyType.COLORED_BALANCE, balances));
        }
    }
}
