package org.eontechnology.and.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.Map;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.AccountProperty;
import org.eontechnology.and.peer.eon.PropertyType;
import org.eontechnology.and.peer.eon.ledger.AccountPropertyDeserializer;
import org.eontechnology.and.peer.eon.ledger.state.BalanceProperty;

public class BalancePropertyDeserializer extends AccountPropertyDeserializer {

  @Override
  public Object deserialize(Account account) throws IOException {

    AccountProperty p = account.getProperty(PropertyType.BALANCE);
    if (p == null) {
      return new BalanceProperty();
    }

    Map<String, Object> map = p.getData();
    try {
      long balance = Long.parseLong(String.valueOf(map.get("amount")));
      return new BalanceProperty(balance);
    } catch (NumberFormatException e) {
      throw new IOException(e);
    }
  }
}
