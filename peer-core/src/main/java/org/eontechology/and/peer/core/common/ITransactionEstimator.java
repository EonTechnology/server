package org.eontechology.and.peer.core.common;

import org.eontechology.and.peer.core.data.Transaction;

public interface ITransactionEstimator {
    /**
     * Calculates and returns the difficulty of the transaction.
     *
     * @param tx transaction
     * @return
     */
    int estimate(Transaction tx);
}
