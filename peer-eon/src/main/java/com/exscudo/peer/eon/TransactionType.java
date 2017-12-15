package com.exscudo.peer.eon;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * Registration of known types of transactions.
 */
public class TransactionType {

	/**
	 * Registration of a new account in the system.
	 */
	public static final int AccountRegistration = 100;

	/**
	 * Transfer of coins between two accounts.
	 */
	public static final int OrdinaryPayment = 200;

	/**
	 * Add funds to deposit to participate in the generation of blocks.
	 */
	public static final int DepositRefill = 310;

	/**
	 * Decrease funds from deposit
	 */
	public static final int DepositWithdraw = 320;

	/**
	 * Sets the weight of the voice for the signature
	 */
	public static final int Delegate = 425;

	/**
	 * Sets the quorum for the transaction
	 */
	public static final int Quorum = 400;

	/**
	 * Refusal to participate in transaction confirmation
	 */
	public static final int Rejection = 450;

	/**
	 * Allows uses the account to the public mode. For this, a SEED is published.
	 * <p>
	 * ATTENTION: transaction is irreversible When you create such a transaction and
	 * send it to the server, you will corrupt the SEED for the account. Even if the
	 * transaction was not accepted by the network, it is necessary to proceed from
	 * the fact that the key is compromised. Therefore, it is important that a
	 * transaction is sent for an account whose weight is 0.
	 */
	public static final int AccountPublication = 475;

	private static Set<Integer> types = null;
	private static final int MODIFIER = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;

	private static void init() {
		try {
			types = new HashSet<>();
			Class<?> clazz = TransactionType.class;
			for (Field f : clazz.getFields()) {
				if ((f.getModifiers() & MODIFIER) == MODIFIER) {
					types.add(Integer.parseInt(String.valueOf(f.get(null))));
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns true if the specified {@code type} is exist.
	 *
	 * @param type
	 *            for validation
	 * @return true if known type, otherwise - false
	 */
	public static boolean contains(int type) {
		if (types == null) {
			init();
		}
		return types.contains(type);
	}

	/**
	 * Returns known type.
	 *
	 * @return
	 */
	public static Integer[] getTypes() {
		if (types == null) {
			init();
		}
		return types.toArray(new Integer[0]);
	}

}
