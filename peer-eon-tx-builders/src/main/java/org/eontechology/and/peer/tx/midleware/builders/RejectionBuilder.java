package org.eontechology.and.peer.tx.midleware.builders;

import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.tx.TransactionType;

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
