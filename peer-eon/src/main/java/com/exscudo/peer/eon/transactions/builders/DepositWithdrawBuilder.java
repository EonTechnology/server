package com.exscudo.peer.eon.transactions.builders;

import com.exscudo.peer.eon.TransactionType;

/**
 * "Deposit-Withdraw" transaction.
 * <p>
 * Deposit is a blocked balance that participate in the generation of blocks.
 */
public class DepositWithdrawBuilder extends TransactionBuilder<DepositWithdrawBuilder> {

	/**
	 * Determines the deposit size.
	 */
	// TODO: review in MainNet
	public static final long DEPOSIT_TRANSACTION_FEE = 10;

	private DepositWithdrawBuilder() {
		super(TransactionType.DepositWithdraw);
	}

	public static DepositWithdrawBuilder createNew(long amount) {
		return new DepositWithdrawBuilder().withParam("amount", amount).forFee(DEPOSIT_TRANSACTION_FEE);
	}
}
