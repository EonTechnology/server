package com.exscudo.eon.app.utils;

import com.exscudo.eon.app.cfg.ITransactionEstimator;
import com.exscudo.eon.app.utils.mapper.TransaportTransactionMapper;
import com.exscudo.peer.core.crypto.IFormatter;
import com.exscudo.peer.core.data.Transaction;

public class TransactionEstimator implements ITransactionEstimator {
    private final IFormatter formatter;

    public TransactionEstimator(IFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public int estimate(Transaction tx) {

        byte[] bytes = formatter.getBytes(TransaportTransactionMapper.convert(tx));
        return bytes.length;
    }
}
