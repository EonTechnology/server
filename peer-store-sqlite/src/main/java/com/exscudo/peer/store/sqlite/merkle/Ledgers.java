package com.exscudo.peer.store.sqlite.merkle;

import java.util.Objects;

import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.store.sqlite.ConnectionProxy;

/**
 * Factory and utility methods for {@code ILedger} implementations.
 * 
 */
public class Ledgers {

	public static Ledger newReadOnlyLedger(ConnectionProxy connection, byte[] snapshot) {

		Objects.requireNonNull(connection);
		Objects.requireNonNull(snapshot);
		return new Ledger(connection, snapshot) {
			@Override
			public void putAccount(IAccount account) {
				throw new UnsupportedOperationException();
			}
		};
	}

	public static Ledger newLeger(ConnectionProxy connection, byte[] snapshot) {

		Objects.requireNonNull(connection);
		return new Ledger(connection, snapshot);

	}

	public static CachedLedger newCachedLedger(ConnectionProxy connection, byte[] snapshot) {

		Objects.requireNonNull(connection);
		Objects.requireNonNull(snapshot);
		return new CachedLedger(connection, snapshot);

	}

}
