package com.exscudo.peer.store.sqlite;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;

/**
 * Basic implementation of {@code IAccount}.
 *
 * @see IAccount
 */
public class Account implements IAccount {
	private final long id;
	private final Map<UUID, AccountProperty> properties = new HashMap<>();

	public Account(long id) {
		this.id = id;
	}

	public Account(long id, AccountProperty[] props) {
		this.id = id;
		for (AccountProperty p : props) {
			this.properties.put(p.getType(), p);
		}
	}

	public Account(Account account) {

		this(account.getID(), account.getProperties().toArray(new AccountProperty[0]));
	}

	@Override
	public long getID() {
		return id;
	}

	@Override
	public AccountProperty getProperty(UUID uuid) {
		return properties.get(uuid);
	}

	@Override
	public void putProperty(AccountProperty prop) {
		properties.put(prop.getType(), prop);
	}

	@Override
	public Collection<AccountProperty> getProperties() {
		return properties.values();
	}

}
