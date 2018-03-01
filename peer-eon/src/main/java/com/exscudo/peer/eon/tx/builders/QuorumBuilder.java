package com.exscudo.peer.eon.tx.builders;

import java.util.HashMap;

import com.exscudo.peer.core.TransactionType;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;

/**
 * Determines the data necessary for a specific type of transaction to be
 * accepted into the network
 */
public class QuorumBuilder extends TransactionBuilder<QuorumBuilder> {

    private QuorumBuilder() {
        super(TransactionType.Quorum, new HashMap<>());
    }

    public static QuorumBuilder createNew(int quorum) {
        if (quorum < ValidationModeProperty.MIN_QUORUM || quorum > ValidationModeProperty.MAX_QUORUM) {
            throw new IllegalArgumentException();
        }
        return new QuorumBuilder().withParam("all", quorum);
    }

    public QuorumBuilder quorumForType(int type, int quorum) {
        if (quorum < ValidationModeProperty.MIN_QUORUM || quorum >= ValidationModeProperty.MAX_QUORUM) {
            throw new IllegalArgumentException("data");
        }
        if (!TransactionType.contains(type)) {
            throw new IllegalArgumentException("type");
        }
        withParam(Integer.toString(type), quorum);
        return this;
    }
}
