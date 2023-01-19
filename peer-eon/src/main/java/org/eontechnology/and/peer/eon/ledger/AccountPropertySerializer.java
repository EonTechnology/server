package org.eontechnology.and.peer.eon.ledger;

import java.io.IOException;
import java.util.Objects;
import org.eontechnology.and.peer.core.data.Account;

/**
 * Base class for property deserializer.
 *
 * @param <TValue> type of property
 */
public abstract class AccountPropertySerializer<TValue> {

  private Class<TValue> handledType;

  public AccountPropertySerializer(Class<TValue> handledType) {
    this.handledType = handledType;
  }

  public Class<?> handledType() {
    return handledType;
  }

  public Account serialize(Object obj, Account account) throws IOException {
    Objects.requireNonNull(obj);
    if (!handledType.isInstance(obj)) {
      throw new ClassCastException();
    }
    return doSerialize(handledType.cast(obj), account);
  }

  public abstract Account doSerialize(TValue value, Account account) throws IOException;
}
