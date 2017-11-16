package com.exscudo.peer.eon.transactions;

import java.util.HashMap;

import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.TransactionType;

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

	public static TransactionBuilder refill(long amount) {
		HashMap<String, Object> hashMap = new HashMap<>();
		hashMap.put("amount", amount);

		return new TransactionBuilder(TransactionType.DepositRefill, hashMap).forFee(DEPOSIT_TRANSACTION_FEE);
	}

	public static TransactionBuilder withdraw(long amount) {
		HashMap<String, Object> hashMap = new HashMap<>();
		hashMap.put("amount", amount);

		return new TransactionBuilder(TransactionType.DepositWithdraw, hashMap).forFee(DEPOSIT_TRANSACTION_FEE);
	}

}
