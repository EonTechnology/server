package com.exscudo.peer.eon.tx.builders;

import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.crypto.ISigner;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;

/**
 * Constructor to build a new transaction. <b> The allows base fields of the
 * resulting transaction to be configured
 */
public class TransactionBuilder<TConcreteBuilder extends TransactionBuilder<?>> {

    private long fee = 10;
    private int timestamp = (int) (System.currentTimeMillis() / 1000L);
    private int deadline = 60 * 60;
    private int version = 1;
    private int type;
    private Map<String, Object> data;
    private String note = null;

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
        this.data.put(name, value);

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

    public Transaction build(ISigner signer) throws Exception {

        Transaction tx = new Transaction();
        tx.setType(type);
        tx.setVersion(version);
        tx.setTimestamp(timestamp);
        tx.setDeadline(deadline);
        tx.setReference(null);
        tx.setSenderID(new AccountID(signer.getPublicKey()));
        tx.setFee(fee);
        tx.setData(data);
        tx.setNote(note);

        byte[] bytes = tx.getBytes();
        byte[] signature = signer.sign(bytes);
        tx.setSignature(signature);

        return tx;
    }

    public Transaction build(ISigner signer, ISigner[] delegates) throws Exception {
        Transaction tx = build(signer);
        byte[] bytes = tx.getBytes();
        HashMap<String, Object> confirmation = new HashMap<>();
        for (ISigner s : delegates) {
            AccountID id = new AccountID(s.getPublicKey());
            byte[] signature = s.sign(bytes);
            confirmation.put(id.toString(), Format.convert(signature));
        }
        tx.setConfirmations(confirmation);
        return tx;
    }
}
