package org.eontechnology.and.peer.core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.common.TimeProvider;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.data.identifier.TransactionID;

public class Builder {

  private long fee = 10;
  private int timestamp;
  private int deadline = 60 * 60;
  private int version = 1;
  private int type = 1;
  private Map<String, Object> data = new HashMap<>();
  private String note;
  private List<Transaction> nestedTransaction;
  private TransactionID referenceID;
  private AccountID payerID;

  public static Builder newTransaction(TimeProvider timeProvider) {
    return newTransaction(timeProvider.get());
  }

  public static Builder newTransaction(int timestamp) {
    Builder builder = new Builder();
    builder.timestamp = timestamp;
    return builder;
  }

  public Builder deadline(short deadline) {
    this.deadline = deadline;
    return this;
  }

  public Builder type(int type) {
    this.type = type;
    return this;
  }

  public Builder forFee(long fee) {
    this.fee = fee;
    return this;
  }

  public Builder attach(Map<String, Object> map) {
    this.data = map;
    return this;
  }

  public Builder addNested(Transaction nestedTx) {
    if (nestedTransaction == null) {
      nestedTransaction = new LinkedList<>();
    }
    this.nestedTransaction.add(nestedTx);
    return this;
  }

  public Builder version(int version) {
    this.version = version;
    return this;
  }

  public Builder note(String note) {
    this.note = note;
    return this;
  }

  public Builder refBy(TransactionID transactionID) {
    this.referenceID = transactionID;
    return this;
  }

  public Builder payedBy(AccountID payerID) {
    this.payerID = payerID;
    return this;
  }

  public Transaction build(BlockID networkID, ISigner signer) throws Exception {

    Transaction tx = new Transaction();
    tx.setType(type);
    tx.setVersion(version);
    tx.setTimestamp(timestamp);
    tx.setDeadline(deadline);
    tx.setReference(referenceID);
    tx.setPayer(payerID);
    tx.setSenderID(new AccountID(signer.getPublicKey()));
    tx.setFee(fee);
    tx.setData(data);
    tx.setNote(note);

    if (nestedTransaction != null) {
      Map<String, Transaction> map = new HashMap<>();
      for (Transaction nestedTx : nestedTransaction) {
        map.put(nestedTx.getID().toString(), nestedTx);
      }
      tx.setNestedTransactions(map);
    }

    byte[] signature = signer.sign(tx, networkID);
    tx.setSignature(signature);

    return tx;
  }

  public Transaction build(BlockID networkID, ISigner signer, ISigner[] delegates)
      throws Exception {
    Transaction tx = build(networkID, signer);
    HashMap<String, Object> confirmation = new HashMap<>();
    for (ISigner s : delegates) {
      AccountID id = new AccountID(s.getPublicKey());
      byte[] signature = s.sign(tx, networkID);
      confirmation.put(id.toString(), Format.convert(signature));
    }
    tx.setConfirmations(confirmation);
    return tx;
  }
}
