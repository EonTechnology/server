package com.exscudo.peer.eon;

import java.io.IOException;
import java.util.Objects;

import com.exscudo.peer.core.services.IAccount;

/**
 * Base class for property deserializer.
 * 
 * @param <TValue>
 *            type of property
 */
public abstract class AccountPropertySerializer<TValue> {

	private Class<TValue> handledType;

	public AccountPropertySerializer(Class<TValue> handledType) {
		this.handledType = handledType;
	}

	public Class<?> handledType() {
		return handledType;
	}

	public void serialize(Object obj, IAccount account) throws IOException {
		Objects.requireNonNull(obj);
		if (!handledType.isInstance(obj)) {
			throw new ClassCastException();
		}
		doSerialize(handledType.cast(obj), account);
	}

	public abstract void doSerialize(TValue value, IAccount account) throws IOException;

}
