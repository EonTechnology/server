package com.exscudo.peer.eon.transactions.builders;

import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.state.ValidationMode;

/**
 * Specifies the weight for a particular account when using a multi-signature.
 */
public class DelegateBuilder extends TransactionBuilder<DelegateBuilder> {

	private DelegateBuilder() {
		super(TransactionType.Delegate);
	}

	public static DelegateBuilder createNew(long accountID, int weight) {
		if (weight < ValidationMode.MIN_WEIGHT || weight > ValidationMode.MAX_WEIGHT) {
			throw new IllegalArgumentException();
		}
		return new DelegateBuilder().withParam(Format.ID.accountId(accountID), weight);
	}

	public static DelegateBuilder createNew(long accountID) {
		return createNew(accountID, 0);
	}

}
