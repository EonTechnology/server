package com.exscudo.peer.eon.transactions;

import java.util.HashMap;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.crypto.ISigner;

/**
 * "Registration" transaction.
 * <p>
 * Publication of the public key. The public key is used to form the recipient
 * field.
 */
public class Registration {

	public static AccountBuilder newAccount(byte[] publicKey) {
		return new AccountBuilder(publicKey);
	}

	public static class AccountBuilder {
		private int timestamp;
		private short deadline;
		private byte[] publicKey;
		private long fee;

		public AccountBuilder(byte[] publicKey) {
			this.publicKey = publicKey;
			this.deadline = 60;
		}

		public AccountBuilder validity(int timestamp, short deadline) {
			this.deadline = deadline;
			this.timestamp = timestamp;
			return this;
		}

		public AccountBuilder forFee(long fee) {
			this.fee = fee;
			return this;
		}

		public Transaction build(ISigner signer) {

			Transaction tx = new Transaction();
			tx.setType(TransactionType.AccountRegistration);
			tx.setTimestamp(timestamp);
			tx.setDeadline(deadline);
			tx.setReference(0);
			tx.setSenderID(Format.MathID.pick(signer.getPublicKey()));
			tx.setFee(fee);

			HashMap<String, Object> hashMap = new HashMap<>();
			hashMap.put(Format.ID.accountId(Format.MathID.pick(publicKey)), Format.convert(publicKey));
			tx.setData(hashMap);

			byte[] bytes = tx.getBytes();
			byte[] signature = signer.sign(bytes);
			tx.setSignature(signature);

			return tx;

		}
	}
}
