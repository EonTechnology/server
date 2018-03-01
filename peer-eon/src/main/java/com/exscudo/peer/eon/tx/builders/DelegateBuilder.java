package com.exscudo.peer.eon.tx.builders;

import com.exscudo.peer.core.TransactionType;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;

/**
 * Specifies the weight for a particular account when using a multi-signature.
 */
public class DelegateBuilder extends TransactionBuilder<DelegateBuilder> {

    private DelegateBuilder() {
        super(TransactionType.Delegate);
    }

    public static DelegateBuilder createNew(AccountID accountID, int weight) {
        if (weight < ValidationModeProperty.MIN_WEIGHT || weight > ValidationModeProperty.MAX_WEIGHT) {
            throw new IllegalArgumentException();
        }
        return new DelegateBuilder().withParam(accountID.toString(), weight);
    }

    public static DelegateBuilder createNew(AccountID accountID) {
        return createNew(accountID, 0);
    }
}
