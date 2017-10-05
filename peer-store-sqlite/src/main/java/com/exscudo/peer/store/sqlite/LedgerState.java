package com.exscudo.peer.store.sqlite;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.store.sqlite.utils.AccountHelper;

/**
 * Implementation of {@code LedgerBase} with direct connection to DB.
 * 
 * @see LedgerBase
 */
public class LedgerState extends LedgerBase {
	private final Storage connector;
	private final Block block;

	public LedgerState(Storage connector, Block block) {
		this.connector = connector;
		this.block = block;
	}

	@Override
	Account findAccount(long accountID) {
		return AccountHelper.getAccount(connector.getConnection(), accountID, block.getHeight());
	}

}