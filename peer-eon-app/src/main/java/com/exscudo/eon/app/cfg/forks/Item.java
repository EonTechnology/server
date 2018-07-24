package com.exscudo.eon.app.cfg.forks;

import java.util.Set;

import com.exscudo.peer.core.crypto.CryptoProvider;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.middleware.ITransactionParser;

public interface Item {

    boolean isCome(int timestamp);

    boolean isPassed(int timestamp);

    int getNumber();

    Set<Integer> getTransactionTypes();

    int getBlockVersion();

    CryptoProvider getCryptoProvider();

    Account convert(Account account);

    boolean needConvertAccounts();

    long getBegin();

    void setBegin(long begin);

    long getEnd();

    void setEnd(long end);

    ITransactionParser getParser();
}
