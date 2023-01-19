package org.eontechnology.and.peer.tx;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/** Registration of known types of transactions. */
public class TransactionType {

  /** Registration of a new account in the system. */
  public static final int Registration = 100;

  /** Transfer of coins between two accounts. */
  public static final int Payment = 200;

  /** Set size of funds in deposit to participate in the generation of blocks. */
  public static final int Deposit = 300;

  /** Sets the weight of the voice for the signature */
  public static final int Delegate = 400;

  /** Sets the quorum for the transaction */
  public static final int Quorum = 410;

  /** Refusal to participate in transaction confirmation */
  public static final int Rejection = 420;

  /**
   * Allows uses the account to the public mode. For this, a SEED is published.
   *
   * <p>ATTENTION: transaction is irreversible When you create such a transaction and send it to the
   * server, you will corrupt the SEED for the account. Even if the transaction was not accepted by
   * the network, it is necessary to proceed from the fact that the key is compromised. Therefore,
   * it is important that a transaction is sent for an account whose weight is 0.
   */
  public static final int Publication = 430;

  /**
   * Issue of colored coins.
   *
   * <p>Associates an account with a color coin. All operations for the issue or withdrawal of a
   * colored coin will be tied to the current account
   *
   * <p>ATTENTION: A reverse transaction is possible only if the entire amount of colored coins on
   * the account balance.
   */
  public static final int ColoredCoinRegistration = 500;

  /** Transfer of colored coins between two accounts. */
  public static final int ColoredCoinPayment = 510;

  /** Sets the total number of colored coins. */
  public static final int ColoredCoinSupply = 520;

  /**
   * Remove a colored coin.
   *
   * <p>To remove a colored coin, the total amount of money should be set to zero. The entire amount
   * of funds must be on the balance of the sender
   */
  public static final int ColoredCoinRemove = 530;

  /**
   * A sequence of activities that must be performed atomically.
   *
   * <p>The sequence of actions is determined by Reference field.
   */
  public static final int ComplexPayment = 600;

  private static final int MODIFIER = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
  private static HashMap<String, Integer> types = null;

  private static void init() {
    try {
      types = new HashMap<>();
      Class<?> clazz = TransactionType.class;
      for (Field f : clazz.getFields()) {
        if ((f.getModifiers() & MODIFIER) == MODIFIER) {
          types.put(f.getName(), Integer.parseInt(String.valueOf(f.get(null))));
        }
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns true if the specified {@code type} is exist.
   *
   * @param typeCode for validation
   * @return true if known type, otherwise - false
   */
  public static boolean contains(int typeCode) {
    if (types == null) {
      init();
    }
    return types.values().contains(typeCode);
  }

  /**
   * Returns true if the specified {@code type} is exist.
   *
   * @param typeName for validation
   * @return true if known type, otherwise - false
   */
  public static boolean contains(String typeName) {
    if (types == null) {
      init();
    }
    return types.containsKey(typeName);
  }

  /**
   * Returns code for the specified type
   *
   * @param typeName type name
   * @return code or null if type not exist
   */
  public static Integer getType(String typeName) {
    if (types == null) {
      init();
    }
    return types.get(typeName);
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
    return types.values().toArray(new Integer[0]);
  }
}
