package com.exscudo.peer.store.sqlite;

import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;

/**
 * Base implementation of {@code ILedger}.
 * <p>
 * Instead of returning IAccount in {@link ILedger#getAccount} returns its
 * implementation in {@link LedgerBase#findAccount}.
 *
 * @see ILedger
 * @see IAccount
 * @see Account
 */
public abstract class LedgerBase implements ILedger {

	abstract Account findAccount(long accountID);

	@Override
	public IAccount getAccount(long accountID) {
		return findAccount(accountID);
	}
}
