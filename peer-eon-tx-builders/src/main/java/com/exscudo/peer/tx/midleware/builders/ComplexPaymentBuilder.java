package com.exscudo.peer.tx.midleware.builders;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.tx.TransactionType;

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
