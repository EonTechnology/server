package com.exscudo.eon.peer.contract;

import java.util.Map;

import com.exscudo.eon.peer.data.Block;
import com.exscudo.eon.peer.data.Transaction;

public class TransportableBlock {

	public final Block block;
	public final Map<Long, Transaction> transactions;
	
	public TransportableBlock(Block block, Map<Long, Transaction> transactions) {
		this.block = block;
		this.transactions = transactions;
	}

}
