package com.exscudo.peer.store.sqlite.core;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.ISandbox;
import com.exscudo.peer.core.services.ITransactionHandler;
import com.exscudo.peer.core.services.LinkedBlock;
import com.exscudo.peer.store.sqlite.LedgerBase;
import com.exscudo.peer.store.sqlite.LedgerProxy;
import com.exscudo.peer.store.sqlite.Sandbox;

/**
 * Basic implementation of {@code LinkedBlock}
 *
 * @see LinkedBlock
 */
public class LinkedBlockImpl extends LinkedBlock {
	private static final long serialVersionUID = 1L;

	private final LedgerBase ledger;

	public LinkedBlockImpl(LedgerBase ledger, Block block) {

		this.setAccProps(block.getAccProps());
		this.setCumulativeDifficulty(block.getCumulativeDifficulty());
		this.setGenerationSignature(block.getGenerationSignature());
		this.setHeight(block.getHeight());
		this.setNextBlock(block.getNextBlock());
		this.setPreviousBlock(block.getPreviousBlock());
		this.setSenderID(block.getSenderID());
		this.setSignature(block.getSignature());
		this.setTimestamp(block.getTimestamp());
		this.setTransactions(block.getTransactions());
		this.setVersion(block.getVersion());

		this.ledger = ledger;
	}

	@Override
	public ILedger getState() {
		return ledger;
	}

	@Override
	public ISandbox createSandbox(ITransactionHandler handler) {
		LedgerProxy proxy = new LedgerProxy(this, ledger);
		return new Sandbox(proxy, handler);
	}

}
