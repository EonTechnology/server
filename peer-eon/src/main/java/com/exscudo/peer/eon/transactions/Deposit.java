package com.exscudo.peer.eon.transactions;

import java.util.HashMap;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.crypto.ISigner;

/**
 * "Deposit" transaction.
 * <p>
 * Deposit is a blocked balance that participate in the generation of blocks.
 */
public class Deposit {

	/**
	 * Determines the deposit size.
	 */
	// TODO: review in MainNet
	public static final long DEPOSIT_TRANSACTION_FEE = 10;

	/**
	 * Determines the minimal amount of deposit witch required to participate in the
	 * generation of blocks.
	 */
	public static final long DEPOSIT_MIN_SIZE_FOR_GEN = EonConstant.MIN_DEPOSIT_SIZE;

	public static DepositRefillBuilder refill(long amount) {
		return new DepositRefillBuilder(amount);
	}

	public static DepositWithdrawBuilder withdraw(long amount) {
		return new DepositWithdrawBuilder(amount);
	}

	public static class DepositRefillBuilder {
		private final long amount;
		private int timestamp;
		private short deadline;

		DepositRefillBuilder(long amount) {
			this.amount = amount;
		}

		public DepositRefillBuilder validity(int timestamp, short deadline) {
			this.deadline = deadline;
			this.timestamp = timestamp;
			return this;
		}

		public Transaction build(ISigner signer) throws Exception {

			Transaction tx = new Transaction();
			tx.setType(TransactionType.DepositRefill);
			tx.setTimestamp(timestamp);
			tx.setDeadline(deadline);
			tx.setSenderID(Format.MathID.pick(signer.getPublicKey()));
			tx.setFee(DEPOSIT_TRANSACTION_FEE);

			HashMap<String, Object> hashMap = new HashMap<>();
			hashMap.put("amount", amount);
			tx.setData(hashMap);

			byte[] bytes = tx.getBytes();
			byte[] signature = signer.sign(bytes);
			tx.setSignature(signature);

			return tx;

		}
	}

	public static class DepositWithdrawBuilder {
		private final long amount;
		private int timestamp;
		private short deadline;

		public DepositWithdrawBuilder(long amount) {
			this.amount = amount;
		}

		public DepositWithdrawBuilder validity(int timestamp, short deadline) {
			this.deadline = deadline;
			this.timestamp = timestamp;
			return this;
		}

		public Transaction build(ISigner signer) throws Exception {

			Transaction tx = new Transaction();
			tx.setType(TransactionType.DepositWithdraw);
			tx.setTimestamp(timestamp);
			tx.setDeadline(deadline);
			tx.setSenderID(Format.MathID.pick(signer.getPublicKey()));
			tx.setFee(DEPOSIT_TRANSACTION_FEE);

			HashMap<String, Object> hashMap = new HashMap<>();
			hashMap.put("amount", amount);
			tx.setData(hashMap);

			byte[] bytes = tx.getBytes();
			byte[] signature = signer.sign(bytes);
			tx.setSignature(signature);

			return tx;

		}
	}

}
