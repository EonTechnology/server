package org.eontechnology.and.eon.app.jsonrpc.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Map;
import org.eontechnology.and.eon.app.utils.mapper.AccountMapper;
import org.eontechnology.and.peer.core.data.Account;

public class AccountDeserializer extends StdDeserializer<Account> {
  public AccountDeserializer() {
    super(Account.class);
  }

  @Override
  public Account deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException, JsonProcessingException {
    Map<String, Object> map = jsonParser.readValueAs(new TypeReference<Map<String, Object>>() {});

    try {
      return AccountMapper.convert(map);
    } catch (IllegalArgumentException ignored) {

    }
    return null;
  }
}
