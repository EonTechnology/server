package com.exscudo.peer.eon.tx.builders;

import java.util.HashMap;

import com.exscudo.peer.core.TransactionType;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.eon.EonConstant;

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
        if (amount < 0 || amount > EonConstant.MAX_MONEY) {
            throw new IllegalArgumentException("amount");
        }
        return new PaymentBuilder().withParam("amount", amount).withParam("recipient", recipient.toString());
    }
}
