package com.exscudo.peer.eon;

import java.io.IOException;

import com.exscudo.peer.core.services.IAccount;

/**
 * Base class for property serializer.
 */
public abstract class AccountPropertyDeserializer {

	public abstract Object deserialize(IAccount account) throws IOException;

}
