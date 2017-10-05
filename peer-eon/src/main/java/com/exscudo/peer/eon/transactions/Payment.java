package com.exscudo.peer.eon.transactions;

import java.util.HashMap;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.crypto.ISigner;

/**
 * "Payment" transaction.
 * <p>
 * Transfers coins between accounts.
 */
public class Payment {
	/**
	 * The maximum possible payment.
	 */
	public static final long MAX_PAYMENT = EonConstant.MAX_MONEY;

	/**
	 * Minimum allowable payment.
	 */
	public static final long MIN_PAYMENT = 0;

	public static OrdinaryPayment newPayment(long amount) {

		if (amount < MIN_PAYMENT || amount > MAX_PAYMENT) {
			throw new IllegalArgumentException("amount");
		}

		return new OrdinaryPayment(amount);

	}

	public static class OrdinaryPayment {

		private final long amount;
		private long fee;
		private int timestamp;
		private short deadline;
		private Long recipient;

		public OrdinaryPayment(long amount) {

			this.amount = amount;
			this.fee = EonConstant.TRANSACTION_MIN_FEE;
		}

		public OrdinaryPayment to(long accountID) {
			this.recipient = accountID;
			return this;
		}

		public OrdinaryPayment forFee(long fee) {
			this.fee = fee;
			return this;
		}

		public OrdinaryPayment validity(int timestamp, short deadline) {
			this.deadline = deadline;
			this.timestamp = timestamp;

			return this;
		}

		public Transaction build(ISigner signer) throws Exception {

			if (recipient == null) {
				throw new Exception("No payment recipient.");
			}

			Transaction tx = new Transaction();
			tx.setType(TransactionType.OrdinaryPayment);
			tx.setTimestamp(timestamp);
			tx.setDeadline(deadline);
			tx.setReference(0);
			tx.setSenderID(Format.MathID.pick(signer.getPublicKey()));
			tx.setFee(fee);

			HashMap<String, Object> hashMap = new HashMap<>();
			hashMap.put("amount", amount);
			hashMap.put("recipient", Format.ID.accountId(recipient));
			tx.setData(hashMap);

			byte[] bytes = tx.getBytes();
			byte[] signature = signer.sign(bytes);
			tx.setSignature(signature);

			return tx;

		}
	}

}
