package org.eontechnology.and.peer.eon.ledger.state.serialization;

import java.io.IOException;
import java.util.HashMap;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.AccountProperty;
import org.eontechnology.and.peer.eon.PropertyType;
import org.eontechnology.and.peer.eon.ledger.AccountPropertySerializer;
import org.eontechnology.and.peer.eon.ledger.state.BalanceProperty;

public class BalancePropertySerializer extends AccountPropertySerializer<BalanceProperty> {

  public BalancePropertySerializer() {
    super(BalanceProperty.class);
  }

  @Override
  public Account doSerialize(BalanceProperty balance, Account account) throws IOException {

    if (balance.getValue() != 0L) {
      HashMap<String, Object> data = new HashMap<>();
      data.put("amount", balance.getValue());

      return account.putProperty(new AccountProperty(PropertyType.BALANCE, data));
    } else {
      return account.removeProperty(PropertyType.BALANCE);
    }
  }
}
