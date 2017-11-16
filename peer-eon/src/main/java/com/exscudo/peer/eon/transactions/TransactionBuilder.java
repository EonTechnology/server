package com.exscudo.peer.eon.transactions;

import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.crypto.ISigner;

/**
 * Constructor to build a new transaction.
 * <b>
 * The allows base fields of the resulting transaction to be configured
 */
public class TransactionBuilder {

	private long fee = EonConstant.TRANSACTION_MIN_FEE;
	private int timestamp = (int) (System.currentTimeMillis() / 1000L);
	private int deadline = 60;
	private int version = 2;
	private int type;
	private Map<String, Object> data;

	public TransactionBuilder(int type, Map<String, Object> data) {
		this.type = type;
		this.data = data;
	}

	public TransactionBuilder forFee(long fee) {
		this.fee = fee;
		return this;
	}

	public TransactionBuilder validity(int timestamp, int deadline) {

		return validity(timestamp, deadline, this.version);
	}

	public TransactionBuilder validity(int timestamp, int deadline, int version) {
		this.deadline = deadline;
		this.timestamp = timestamp;
		this.version = version;

		return this;
	}

	public Transaction build(ISigner signer) throws Exception {

		Transaction tx = new Transaction();
		tx.setType(type);
		tx.setVersion(version);
		tx.setTimestamp(timestamp);
		tx.setDeadline(deadline);
		tx.setReference(0);
		tx.setSenderID(Format.MathID.pick(signer.getPublicKey()));
		tx.setFee(fee);
		tx.setData(data);

		byte[] bytes = tx.getBytes();
		byte[] signature = signer.sign(bytes);
		tx.setSignature(signature);

		return tx;

	}

}
