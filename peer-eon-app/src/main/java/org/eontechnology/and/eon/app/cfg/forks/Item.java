package org.eontechnology.and.eon.app.cfg.forks;

import java.util.Set;
import org.eontechnology.and.peer.core.crypto.CryptoProvider;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;

public interface Item {

  boolean isCome(int timestamp);

  boolean isPassed(int timestamp);

  int getNumber();

  Set<Integer> getTransactionTypes();

  int getBlockVersion();

  CryptoProvider getCryptoProvider();

  Account convert(Account account);

  long getBegin();

  void setBegin(long begin);

  long getEnd();

  void setEnd(long end);

  ITransactionParser getParser();

  int getBlockPeriod();

  long getBlockSize();

  int getGenerationSaltVersion();
}
