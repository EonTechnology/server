package com.exscudo.peer.eon.ledger;

import java.io.IOException;

import com.exscudo.peer.core.data.Account;

/**
 * Base class for property serializer.
 */
public abstract class AccountPropertyDeserializer {

    public abstract Object deserialize(Account account) throws IOException;
}
