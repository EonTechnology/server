package com.exscudo.peer.store.sqlite;

import com.exscudo.peer.core.services.AbstractSandbox;
import com.exscudo.peer.core.services.AccountProperty;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ITransactionHandler;

/**
 * Basic implementation of {@code AbstractSandbox}.
 *
 * @see AbstractSandbox
 */
public class Sandbox extends AbstractSandbox {
	private final LedgerProxy ledger;

	public Sandbox(LedgerProxy ledger, ITransactionHandler handler) {
		super(handler);
		this.ledger = ledger;
	}

	@Override
	public AccountProperty[] getProperties() {
		return ledger.getProperties();
	}

	@Override
	protected ILedger getLedger() {
		return ledger;
	}

}
