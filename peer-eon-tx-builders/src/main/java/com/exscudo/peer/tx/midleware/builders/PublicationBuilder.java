package com.exscudo.peer.tx.midleware.builders;

import com.exscudo.peer.tx.TransactionType;

/**
 * Creates an public account.
 * <p>
 * ATTENTION: see {@link TransactionType#Publication} description.
 */
public class PublicationBuilder extends TransactionBuilder<PublicationBuilder> {

    private PublicationBuilder() {
        super(TransactionType.Publication);
    }

    public static PublicationBuilder createNew(String seed) {
        return new PublicationBuilder().withParam("seed", seed);
    }
}
