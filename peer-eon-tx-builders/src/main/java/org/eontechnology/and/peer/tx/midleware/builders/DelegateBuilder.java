package org.eontechnology.and.peer.tx.midleware.builders;

import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.tx.TransactionType;

/**
 * Specifies the weight for a particular account when using a multi-signature.
 */
public class DelegateBuilder extends TransactionBuilder<DelegateBuilder> {

    private DelegateBuilder() {
        super(TransactionType.Delegate);
    }

    public static DelegateBuilder createNew(AccountID accountID, int weight) {
        return new DelegateBuilder().withParam(accountID.toString(), weight);
    }
}
