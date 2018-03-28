package com.exscudo.peer.eon.tx.builders;

import com.exscudo.peer.eon.TransactionType;

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
