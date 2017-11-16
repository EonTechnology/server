package com.exscudo.peer.core.services;

/**
 * Provides an access to accounts and their properties. Defines the state at a
 * certain height of the chain of the blocks.
 * 
 */
public interface ILedger {

	IAccount getAccount(long accountID);

	default boolean existAccount(long accountID) {
		return getAccount(accountID) != null;
	}

	void putAccount(IAccount account);

	byte[] getHash();

}
