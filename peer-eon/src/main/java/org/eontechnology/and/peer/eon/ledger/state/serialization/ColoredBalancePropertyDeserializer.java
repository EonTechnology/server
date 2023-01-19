package org.eontechnology.and.peer.eon.ledger.state.serialization;

import java.io.IOException;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.AccountProperty;
import org.eontechnology.and.peer.eon.PropertyType;
import org.eontechnology.and.peer.eon.ledger.AccountPropertyDeserializer;
import org.eontechnology.and.peer.eon.ledger.state.ColoredBalanceProperty;

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
