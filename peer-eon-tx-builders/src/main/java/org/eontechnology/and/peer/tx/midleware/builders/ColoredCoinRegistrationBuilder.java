package org.eontechnology.and.peer.tx.midleware.builders;

import org.eontechnology.and.peer.tx.TransactionType;

/**
 * Creates a colored coins.
 *
 * <p>Performs binding of a colored coin to an existing account.
 */
public class ColoredCoinRegistrationBuilder
    extends TransactionBuilder<ColoredCoinRegistrationBuilder> {

  private ColoredCoinRegistrationBuilder() {
    super(TransactionType.ColoredCoinRegistration);
  }

  public static ColoredCoinRegistrationBuilder createNew(long emission, int decimalPoint) {
    return new ColoredCoinRegistrationBuilder()
        .withParam("emission", emission)
        .withParam("decimal", decimalPoint);
  }

  public static ColoredCoinRegistrationBuilder createNew(int decimalPoint) {
    return new ColoredCoinRegistrationBuilder()
        .withParam("emission", "auto")
        .withParam("decimal", decimalPoint);
  }
}
