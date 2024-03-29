package org.eontechnology.and.eon.app.cfg.forks;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eontechnology.and.peer.core.crypto.CryptoProvider;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.eon.midleware.CompositeTransactionParser;

public class ForkItem implements Item {

  protected final int number;

  protected long begin;
  protected long end;
  protected Map<Integer, ITransactionParser> transactionTypes = new HashMap<>();
  protected int blockVersion;
  protected CryptoProvider cryptoProvider;
  protected List<String> validationRules;
  protected int blockPeriod;
  protected long blockSize;
  protected int generationSaltVersion;

  public ForkItem(int number, String end) {

    this.number = number;
    this.end = Instant.parse(end).toEpochMilli();

    this.blockVersion = 1;
    this.cryptoProvider = CryptoProvider.getInstance();
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
  public Set<Integer> getTransactionTypes() {
    return transactionTypes.keySet();
  }

  @Override
  public int getBlockVersion() {
    return this.blockVersion;
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

  @Override
  public ITransactionParser getParser() {
    return new CompositeTransactionParser(transactionTypes);
  }

  public void addTxType(int type, ITransactionParser parser) {
    if (transactionTypes.containsKey(type)) {
      throw new IllegalArgumentException("type");
    }
    transactionTypes.put(type, parser);
  }

  public void removeTxType(int type) {
    transactionTypes.remove(type);
  }

  public void setValidationRules(List<String> validationRules) {
    this.validationRules = validationRules;
  }

  public List<String> getValidationRules() {
    return this.validationRules;
  }

  @Override
  public int getBlockPeriod() {
    return blockPeriod;
  }

  public void setBlockPeriod(int blockPeriod) {
    this.blockPeriod = blockPeriod;
  }

  @Override
  public long getBlockSize() {
    return blockSize;
  }

  public void setBlockSize(long blockSize) {
    this.blockSize = blockSize;
  }

  @Override
  public int getGenerationSaltVersion() {
    return generationSaltVersion;
  }

  public void setGenerationSaltVersion(int generationSaltVersion) {
    this.generationSaltVersion = generationSaltVersion;
  }
}
