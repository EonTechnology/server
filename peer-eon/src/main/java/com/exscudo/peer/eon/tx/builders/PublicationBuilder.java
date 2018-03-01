package com.exscudo.peer.eon.tx.builders;

import java.util.Objects;

import com.exscudo.peer.core.TransactionType;

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
        Objects.requireNonNull(seed);
        return new PublicationBuilder().withParam("seed", seed);
    }
}
