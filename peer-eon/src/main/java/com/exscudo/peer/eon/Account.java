package com.exscudo.peer.eon;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;

/**
 * Basic implementation of {@code IAccount}.
 *
 * @see IAccount
 */
public class Account implements IAccount {
	private final long id;
	private final Map<String, AccountProperty> properties = new HashMap<>();

	public Account(long id) {
		this.id = id;
	}

	public Account(long id, AccountProperty[] props) {
		this.id = id;
		for (AccountProperty p : props) {
			this.properties.put(p.getType(), p);
		}
	}

	public Account(IAccount account) {

		this(account.getID(), account.getProperties().toArray(new AccountProperty[0]));
	}

	@Override
	public long getID() {
		return id;
	}

	@Override
	public AccountProperty getProperty(String id) {
		return properties.get(id);
	}

	@Override
	public void putProperty(AccountProperty prop) {
		properties.put(prop.getType(), prop);
	}

	@Override
	public Collection<AccountProperty> getProperties() {
		return properties.values();
	}

	@Override
	public void removeProperty(String id) {
		properties.remove(id);
	}
}
