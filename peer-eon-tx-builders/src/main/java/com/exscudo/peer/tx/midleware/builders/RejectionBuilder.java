package com.exscudo.peer.tx.midleware.builders;

import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.tx.TransactionType;

/**
 * Rejection of the right to confirm transactions of the specified account.
 */
public class RejectionBuilder extends TransactionBuilder<RejectionBuilder> {

    private RejectionBuilder() {
        super(TransactionType.Rejection);
    }

    public static RejectionBuilder createNew(AccountID accountID) {
        return new RejectionBuilder().withParam("account", accountID.toString());
    }
}
