package com.exscudo.peer.tx.midleware.builders;

import java.util.HashMap;

import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.tx.TransactionType;

/**
 * "Payment" transaction.
 * <p>
 * Transfers coins between accounts.
 */
public class PaymentBuilder extends TransactionBuilder<PaymentBuilder> {

    private PaymentBuilder() {
        super(TransactionType.Payment, new HashMap<>());
    }

    public static PaymentBuilder createNew(long amount, AccountID recipient) {
        return new PaymentBuilder().withParam("amount", amount).withParam("recipient", recipient.toString());
    }
}
