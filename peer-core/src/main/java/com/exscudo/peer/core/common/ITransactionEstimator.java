package com.exscudo.peer.core.common;

import com.exscudo.peer.core.data.Transaction;

public interface ITransactionEstimator {
    /**
     * Calculates and returns the difficulty of the transaction.
     *
     * @param tx transaction
     * @return
     */
    int estimate(Transaction tx);
}
