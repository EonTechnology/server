package com.exscudo.peer.eon.transactions;

import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.crypto.ISigner;
import com.exscudo.peer.eon.state.ValidationMode;

public class Quorum {

	/**
	 * Determines the data necessary for a specific type of transaction to be
	 * accepted into the network
	 *
	 * @param quorum
	 * @return
	 */
	public static Builder newQuorum(int quorum) {
		return new Builder(quorum);
	}

	public static class Builder extends TransactionBuilder {

		private Map<String, Object> data = new HashMap<>();

		private Builder(int quorum) {
			super(TransactionType.Quorum);
			if (quorum < ValidationMode.MIN_QUORUM || quorum > ValidationMode.MAX_QUORUM) {
				throw new IllegalArgumentException();
			}
			data.put("all", quorum);
		}

		public Builder quorumForType(int type, int quorum) {
			if (quorum < ValidationMode.MIN_QUORUM || quorum >= ValidationMode.MAX_QUORUM) {
				throw new IllegalArgumentException("data");
			}
			if (!TransactionType.contains(type)) {
				throw new IllegalArgumentException("type");
			}
			data.put(Integer.toString(type), quorum);
			return this;
		}

		@Override
		public Transaction build(ISigner signer) throws Exception {
			setData(data);
			return super.build(signer);
		}
	}

}
