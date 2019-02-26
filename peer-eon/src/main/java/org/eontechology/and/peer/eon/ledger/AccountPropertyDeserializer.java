package org.eontechology.and.peer.eon.ledger;

import java.io.IOException;

import org.eontechology.and.peer.core.data.Account;

/**
 * Base class for property serializer.
 */
public abstract class AccountPropertyDeserializer {

    public abstract Object deserialize(Account account) throws IOException;
}
