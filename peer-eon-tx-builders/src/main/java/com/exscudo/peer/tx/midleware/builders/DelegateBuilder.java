package com.exscudo.peer.tx.midleware.builders;

import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.tx.TransactionType;

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
