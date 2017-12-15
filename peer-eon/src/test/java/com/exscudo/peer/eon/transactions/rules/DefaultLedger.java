package com.exscudo.peer.eon.transactions.rules;

import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;

class DefaultLedger implements ILedger {
	private Map<Long, IAccount> accounts = new HashMap<>();

	@Override
	public IAccount getAccount(long accountID) {
		return accounts.get(accountID);
	}

	@Override
	public void putAccount(IAccount account) {
		accounts.put(account.getID(), account);
	}

	@Override
	public byte[] getHash() {
		throw new UnsupportedOperationException();
	}

}
