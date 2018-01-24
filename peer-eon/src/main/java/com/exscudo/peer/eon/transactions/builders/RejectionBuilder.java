package com.exscudo.peer.eon.transactions.builders;

import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TransactionType;

/**
 * Rejection of the right to confirm transactions of the specified account.
 */
public class RejectionBuilder extends TransactionBuilder<RejectionBuilder> {

	private RejectionBuilder() {
		super(TransactionType.Rejection);
	}

	public static RejectionBuilder createNew(long accountID) {
		return new RejectionBuilder().withParam("account", Format.ID.accountId(accountID));
	}

}
