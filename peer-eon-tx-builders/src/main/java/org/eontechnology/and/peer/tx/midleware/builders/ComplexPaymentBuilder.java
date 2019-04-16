package org.eontechnology.and.peer.tx.midleware.builders;

import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.tx.TransactionType;

public class ComplexPaymentBuilder extends TransactionBuilder<ComplexPaymentBuilder> {

    public ComplexPaymentBuilder() {
        super(TransactionType.ComplexPayment);
    }

    public static ComplexPaymentBuilder createNew(Transaction[] innerTransactions) {
        ComplexPaymentBuilder builder = new ComplexPaymentBuilder();
        for (Transaction tx : innerTransactions) {
            builder.addNested(tx);
        }
        return builder;
    }
}
