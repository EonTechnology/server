package com.exscudo.eon.app.utils;

import com.exscudo.eon.app.cfg.ITransactionEstimator;
import com.exscudo.eon.app.utils.mapper.TransaportTransactionMapper;
import com.exscudo.peer.core.common.BencodeFormatter;
import com.exscudo.peer.core.data.Transaction;

public class TransactionEstimator implements ITransactionEstimator {
    @Override
    public int estimate(Transaction tx) {

        byte[] bytes = (new BencodeFormatter()).getBytes(TransaportTransactionMapper.convert(tx));
        return bytes.length;
    }
}
