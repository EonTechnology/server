package com.exscudo.peer.tx.midleware.builders;

import com.exscudo.peer.tx.TransactionType;

/**
 * Determines the data necessary for a specific type of transaction to be
 * accepted into the network
 */
public class QuorumBuilder extends TransactionBuilder<QuorumBuilder> {

    private QuorumBuilder() {
        super(TransactionType.Quorum);
    }

    public static QuorumBuilder createNew(int quorum) {
        return new QuorumBuilder().withParam("all", quorum);
    }

    public QuorumBuilder quorumForType(int type, int quorum) {
        withParam(Integer.toString(type), quorum);
        return this;
    }
}
