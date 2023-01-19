package org.eontechnology.and.peer.tx.midleware.builders;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.crypto.ISigner;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.data.identifier.TransactionID;

/**
 * Constructor to build a new transaction. <b> The allows base fields of the resulting transaction
 * to be configured
 */
public class TransactionBuilder<TConcreteBuilder extends TransactionBuilder<?>> {

  private long fee = 10;
  private int timestamp = (int) (System.currentTimeMillis() / 1000L);
  private int deadline = 60 * 60;
  private int version = 1;
  private int type;
  private TransactionID reference;
  private Map<String, Object> data;
  private String note = null;
  private List<Transaction> nestedTransaction;
  private AccountID payerID = null;

  public TransactionBuilder(int type, Map<String, Object> data) {
    this.type = type;
    this.data = data;
  }

  public TransactionBuilder(int type) {
    this(type, new HashMap<>());
  }

  public TConcreteBuilder forFee(long fee) {
    this.fee = fee;

    @SuppressWarnings("unchecked")
    TConcreteBuilder cast = (TConcreteBuilder) this;
    return cast;
  }

  public TConcreteBuilder validity(int timestamp, int deadline) {

    return validity(timestamp, deadline, this.version);
  }

  public TConcreteBuilder withParam(String name, Object value) {
    if (value instanceof Number) {
      this.data.put(name, ((Number) value).longValue());
    } else {
      this.data.put(name, value);
    }

    @SuppressWarnings("unchecked")
    TConcreteBuilder cast = (TConcreteBuilder) this;
    return cast;
  }

  public TConcreteBuilder addNote(String note) {
    this.note = note;

    @SuppressWarnings("unchecked")
    TConcreteBuilder cast = (TConcreteBuilder) this;
    return cast;
  }

  public TConcreteBuilder validity(int timestamp, int deadline, int version) {
    this.deadline = deadline;
    this.timestamp = timestamp;
    this.version = version;

    @SuppressWarnings("unchecked")
    TConcreteBuilder cast = (TConcreteBuilder) this;
    return cast;
  }

  public TConcreteBuilder addNested(Transaction nestedTx) {
    if (nestedTransaction == null) {
      nestedTransaction = new LinkedList<>();
    }
    this.nestedTransaction.add(nestedTx);

    @SuppressWarnings("unchecked")
    TConcreteBuilder cast = (TConcreteBuilder) this;
    return cast;
  }

  public TConcreteBuilder refBy(TransactionID transactionID) {
    this.reference = transactionID;

    @SuppressWarnings("unchecked")
    TConcreteBuilder cast = (TConcreteBuilder) this;
    return cast;
  }

  public TConcreteBuilder payedBy(AccountID payerID) {
    this.payerID = payerID;

    @SuppressWarnings("unchecked")
    TConcreteBuilder cast = (TConcreteBuilder) this;
    return cast;
  }

  public Transaction build(BlockID networkID, ISigner signer) throws Exception {

    Transaction tx = new Transaction();
    tx.setType(type);
    tx.setVersion(version);
    tx.setTimestamp(timestamp);
    tx.setDeadline(deadline);
    tx.setReference(null);
    tx.setSenderID(new AccountID(signer.getPublicKey()));
    tx.setFee(fee);
    tx.setReference(reference);
    tx.setData(data);
    tx.setNote(note);
    tx.setPayer(payerID);
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
