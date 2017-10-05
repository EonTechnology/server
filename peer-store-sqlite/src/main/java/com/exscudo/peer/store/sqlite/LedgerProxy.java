package com.exscudo.peer.store.sqlite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.IAccount;

/**
 * Implementation of {@code LedgerBase} with changes of cache.
 *
 * @see LedgerBase
 */
public class LedgerProxy extends LedgerBase {

	private final Block block;
	private final LedgerBase ledger;
	private final Map<Long, Account> accounts = new HashMap<>();

	public LedgerProxy(Block block, LedgerBase ledger) {

		this.block = block;
		this.ledger = ledger;
	}

	@Override
	public Account findAccount(long accountID) {

		if (accountID == Constant.DUMMY_ACCOUNT_ID) {
			return new Account(Constant.DUMMY_ACCOUNT_ID);
		}

		Account wrapper = accounts.get(accountID);
		if (wrapper == null) {

			wrapper = ledger.findAccount(accountID);

			if (wrapper != null) {
				wrapper = new Account(wrapper);
			} else {
				wrapper = new Account(accountID);
			}

			for (AccountProperty property : block.getAccProps()) {
				if (property.getAccountID() == accountID) {
					wrapper.putProperty(property);
				}
			}

			accounts.put(accountID, wrapper);
		}

		if (wrapper.getProperties().size() == 0) {
			return null;
		}

		return wrapper;
	}

	@Override
	public IAccount newAccount(long id) {
		Account newAccount = new Account(new Account(id));
		accounts.put(newAccount.getID(), newAccount);
		return newAccount;
	}

	public AccountProperty[] getProperties() {
		ArrayList<AccountProperty> list = new ArrayList<>();
		for (Account account : accounts.values()) {
			for (AccountProperty prop : account.getProperties()) {
				if (prop.getHeight() < 0) {
					list.add(prop);
				}
			}
		}
		return list.toArray(new AccountProperty[0]);
	}

}
