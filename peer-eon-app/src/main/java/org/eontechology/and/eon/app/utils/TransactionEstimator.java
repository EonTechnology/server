package org.eontechology.and.eon.app.utils;

import org.eontechology.and.eon.app.utils.mapper.TransportTransactionMapper;
import org.eontechology.and.peer.core.common.ITransactionEstimator;
import org.eontechology.and.peer.core.crypto.IFormatter;
import org.eontechology.and.peer.core.data.Transaction;

public class TransactionEstimator implements ITransactionEstimator {
    private final IFormatter formatter;

    public TransactionEstimator(IFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public int estimate(Transaction tx) {

        byte[] bytes = formatter.getBytes(TransportTransactionMapper.convert(tx));
        return bytes.length;
    }
}
