package org.eontechology.and.peer.tx.midleware.builders;

import org.eontechology.and.peer.tx.TransactionType;

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
