package com.exscudo.peer.eon.state.serialization;

public class PropertyType {

	/**
	 * A property that contains a data which set during registration. Required.
	 */
	public static final String REGISTRATION = "00000000-0000-0000-0000-000000000000";

	/**
	 * A property that contains information about account balance. Optional.
	 */
	public static final String BALANCE = "5afe47d6-6233-11e7-907b-a6006ad3dba0";

	/**
	 * A property that contains information about account generation balance.
	 * Optional.
	 */
	public static final String DEPOSIT = "256d84f8-b272-4dcc-a7de-e36b7b8a0da6";

	/**
	 * Contains information about the transactions confirmation method. Optional.
	 */
	public static final String MODE = "mode";

	/**
	 * Contains a list of votings that the account is participated. Optional
	 */
	public static final String VOTER = "voter";

	/**
	 * Contains parameters specified when registering a colored coin. Optional.
	 */
	public static final String COLORED_COIN = "colored-coin";

	/**
	 * Contains a list of coin balances by colors. Optional.
	 */
	public static final String COLORED_BALANCE = "colored-balance";

}
