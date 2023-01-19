package org.eontechnology.and.peer.eon;

public class PropertyType {

  /** A property that contains a data which set during registration. Required. */
  public static final String REGISTRATION = "public-key";

  /** A property that contains information about account balance. Optional. */
  public static final String BALANCE = "balance";

  /** A property that contains information about account generation balance. Optional. */
  public static final String DEPOSIT = "deposit";

  /** Contains information about the transactions confirmation method. Optional. */
  public static final String MODE = "mode";

  /** Contains a list of votings that the account is participated. Optional */
  public static final String VOTER = "voter";

  /** Contains parameters specified when registering a colored coin. Optional. */
  public static final String COLORED_COIN = "colored-coin";

  /** Contains a list of coin balances by colors. Optional. */
  public static final String COLORED_BALANCE = "colored-balance";
}
