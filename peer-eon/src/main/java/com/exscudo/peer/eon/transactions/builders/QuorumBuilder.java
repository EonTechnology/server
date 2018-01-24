package com.exscudo.peer.eon.transactions.builders;

import java.util.HashMap;

import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.state.ValidationMode;

/**
 * Determines the data necessary for a specific type of transaction to be
 * accepted into the network
 */
public class QuorumBuilder extends TransactionBuilder<QuorumBuilder> {

	private QuorumBuilder() {
		super(TransactionType.Quorum, new HashMap<>());
	}

	public QuorumBuilder quorumForType(int type, int quorum) {
		if (quorum < ValidationMode.MIN_QUORUM || quorum >= ValidationMode.MAX_QUORUM) {
			throw new IllegalArgumentException("data");
		}
		if (!TransactionType.contains(type)) {
			throw new IllegalArgumentException("type");
		}
		withParam(Integer.toString(type), quorum);
		return this;
	}

	public static QuorumBuilder createNew(int quorum) {
		if (quorum < ValidationMode.MIN_QUORUM || quorum > ValidationMode.MAX_QUORUM) {
			throw new IllegalArgumentException();
		}
		return new QuorumBuilder().withParam("all", quorum);
	}

}
