package com.exscudo.eon.app.cfg.forks;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.exscudo.eon.app.cfg.ITransactionEstimator;
import com.exscudo.eon.app.utils.TransactionEstimator;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Account;

public class ForkItem implements Item {

    protected final int number;

    protected long begin;
    protected long end;
    protected HashSet<Integer> transactionTypes = new HashSet<>();
    protected int blockVersion;
    protected ITransactionEstimator estimator;
    protected CryptoProvider cryptoProvider;
    protected int maxNoteLength;

    public ForkItem(int number, String begin) {

        this.number = number;
        this.begin = Instant.parse(begin).toEpochMilli();

        this.blockVersion = 1;
        this.maxNoteLength = Constant.TRANSACTION_NOTE_MAX_LENGTH;
        this.cryptoProvider = CryptoProvider.getInstance();
        this.estimator = new TransactionEstimator(this.cryptoProvider.getFormatter());
    }

    @Override
    public boolean isCome(int timestamp) {
        return timestamp * 1000L > begin;
    }

    @Override
    public boolean isPassed(int timestamp) {
        return timestamp * 1000L > end;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public int getMaxNoteLength() {
        return maxNoteLength;
    }

    @Override
    public Set<Integer> getTransactionTypes() {
        return transactionTypes;
    }

    @Override
    public int getBlockVersion() {
        return this.blockVersion;
    }

    @Override
    public ITransactionEstimator getEstimator() {
        return estimator;
    }

    @Override
    public CryptoProvider getCryptoProvider() {
        return this.cryptoProvider;
    }

    @Override
    public Account convert(Account account) {
        return account;
    }

    @Override
    public boolean needConvertAccounts() {
        return false;
    }

    @Override
    public long getBegin() {
        return begin;
    }

    @Override
    public void setBegin(long begin) {
        this.begin = begin;
    }

    @Override
    public long getEnd() {
        return end;
    }

    @Override
    public void setEnd(long end) {
        this.end = end;
    }

    public void addTxType(int type) {
        transactionTypes.add(type);
    }

    public void removeTxType(int type) {
        transactionTypes.remove(type);
    }
}
