package org.eontechnology.and.eon.app.jsonrpc.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.eontechnology.and.eon.app.utils.mapper.AccountMapper;
import org.eontechnology.and.peer.core.data.Account;

public class AccountSerializer extends StdSerializer<Account> {
  public AccountSerializer() {
    super(Account.class);
  }

  @Override
  public void serialize(Account value, JsonGenerator gen, SerializerProvider serializerProvider)
      throws IOException {
    gen.writeObject(AccountMapper.convert(value));
  }
}
