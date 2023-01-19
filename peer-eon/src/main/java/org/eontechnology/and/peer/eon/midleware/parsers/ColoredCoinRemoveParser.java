package org.eontechnology.and.peer.eon.midleware.parsers;

import java.util.Collection;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.eon.midleware.Resources;
import org.eontechnology.and.peer.eon.midleware.actions.ColoredCoinRemoveAction;
import org.eontechnology.and.peer.eon.midleware.actions.FeePaymentAction;
import org.eontechnology.and.peer.tx.ColoredCoinID;

public class ColoredCoinRemoveParser implements ITransactionParser {

  @Override
  public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

    if (transaction.getNestedTransactions() != null) {
      throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
    }

    if (!transaction.getData().isEmpty()) {
      throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
    }

    ColoredCoinID coinID = new ColoredCoinID(transaction.getSenderID().getValue());
    return new ILedgerAction[] {
      new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()),
      new ColoredCoinRemoveAction(coinID)
    };
  }

  @Override
  public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {
    return null;
  }
}
