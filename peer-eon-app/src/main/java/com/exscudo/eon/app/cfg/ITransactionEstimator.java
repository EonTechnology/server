package com.exscudo.eon.app.cfg;

import com.exscudo.peer.core.data.Transaction;

public interface ITransactionEstimator {
    int estimate(Transaction tx);
}
