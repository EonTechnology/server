package com.exscudo.peer.eon.transactions.builders;

import com.exscudo.peer.eon.EonConstant;
import com.exscudo.peer.eon.TransactionType;

/**
 * "Deposit-refill" transaction.
 * <p>
 * Deposit is a blocked balance that participate in the generation of blocks.
 */
public class DepositRefillBuilder extends TransactionBuilder<DepositRefillBuilder> {

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

	private DepositRefillBuilder() {
		super(TransactionType.DepositRefill);
	}

	public static DepositRefillBuilder createNew(long amount) {
		return new DepositRefillBuilder().withParam("amount", amount).forFee(DEPOSIT_TRANSACTION_FEE);
	}

}
