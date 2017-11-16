package com.exscudo.peer.core.services;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * Account property.
 * <p>
 * It is changed by transaction. The specificity of a property depends on the
 * interpretation of a particular type of transaction
 */
public class AccountProperty implements Serializable {
	private static final long serialVersionUID = 2326856545894572608L;

	private UUID type;
	private Map<String, Object> data;

	public AccountProperty(UUID type, Map<String, Object> data) {
		this.type = type;
		this.data = data;
	}

	public Map<String, Object> getData() {
		if (data == null)
			throw new RuntimeException();
		return data;
	}

	public UUID getType() {
		return type;
	}

}
