package org.eontechnology.and.peer.tx.midleware.builders;

import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.tx.TransactionType;

/**
 * "Payment" transaction.
 * <p>
 * Transfers coins between accounts.
 */
public class PaymentBuilder extends TransactionBuilder<PaymentBuilder> {

    private PaymentBuilder() {
        super(TransactionType.Payment);
    }

    public static PaymentBuilder createNew(long amount, AccountID recipient) {
        return new PaymentBuilder().withParam("amount", amount).withParam("recipient", recipient.toString());
    }
}
