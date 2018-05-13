package com.exscudo.peer.tx.midleware.builders;

import com.exscudo.peer.tx.TransactionType;

/**
 * "Deposit-refill" transaction.
 * <p>
 * Deposit is a blocked balance that participate in the generation of blocks.
 */
public class DepositBuilder extends TransactionBuilder<DepositBuilder> {

    private DepositBuilder() {
        super(TransactionType.Deposit);
    }

    public static DepositBuilder createNew(long amount) {
        return new DepositBuilder().withParam("amount", amount);
    }
}
