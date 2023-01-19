package org.eontechnology.and.peer.tx.midleware.builders;

import org.eontechnology.and.peer.tx.TransactionType;

/** Removes a colored coin. */
public class ColoredCoinRemoveBuilder extends TransactionBuilder<ColoredCoinRemoveBuilder> {
  public ColoredCoinRemoveBuilder() {
    super(TransactionType.ColoredCoinRemove);
  }

  public static ColoredCoinRemoveBuilder createNew() {
    return new ColoredCoinRemoveBuilder();
  }
}
