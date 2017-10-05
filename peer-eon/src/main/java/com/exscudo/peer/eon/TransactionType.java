package com.exscudo.peer.eon;

/**
 * Registration of known types of transactions.
 */
public class TransactionType {

	/** Registration of a new account in the system. */
	public static final int AccountRegistration = 100;

	/** Transfer of coins between two accounts. */
	public static final int OrdinaryPayment = 200;

	/** Add funds to deposit to participate in the generation of blocks. */
	public static final int DepositRefill = 310;

	/** Decrease funds from deposit */
	public static final int DepositWithdraw = 320;

}
