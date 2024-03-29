package org.eontechnology.and.peer.eon.midleware.parsers;

import java.util.Collection;
import java.util.Map;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechnology.and.peer.eon.midleware.Resources;
import org.eontechnology.and.peer.eon.midleware.actions.ColoredCoinRegistrationAction;
import org.eontechnology.and.peer.eon.midleware.actions.ColoredCoinSupplyToPresetAction;
import org.eontechnology.and.peer.eon.midleware.actions.EmptyAction;
import org.eontechnology.and.peer.eon.midleware.actions.FeePaymentAction;
import org.eontechnology.and.peer.tx.ColoredCoinID;

public class ColoredCoinRegistrationParserV2 implements ITransactionParser {

  @Override
  public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

    if (transaction.getNestedTransactions() != null) {
      throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
    }

    Map<String, Object> data = transaction.getData();
    if (data.size() != 2) {
      throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
    }

    if (!(data.get("decimal") instanceof Long)) {
      throw new ValidateException(Resources.DECIMAL_POINT_INVALID_FORMAT);
    }

    long decimalPoint = (long) data.get("decimal");
    if (decimalPoint < ColoredCoinProperty.MIN_DECIMAL_POINT
        || decimalPoint > ColoredCoinProperty.MAX_DECIMAL_POINT) {
      throw new ValidateException(Resources.DECIMAL_POINT_OUT_OF_RANGE);
    }

    ILedgerAction emissionAction = new EmptyAction();

    if (!"auto".equals(data.get("emission"))) {

      if (!(data.get("emission") instanceof Long)) {
        throw new ValidateException(Resources.EMISSION_INVALID_FORMAT);
      }

      long moneySupply = (long) data.get("emission");
      if (moneySupply < ColoredCoinProperty.MIN_EMISSION_SIZE) {
        throw new ValidateException(Resources.EMISSION_OUT_OF_RANGE);
      }

      emissionAction =
          new ColoredCoinSupplyToPresetAction(
              new ColoredCoinID(transaction.getSenderID().getValue()), moneySupply);
    }

    return new ILedgerAction[] {
      new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()),
      new ColoredCoinRegistrationAction(transaction.getSenderID(), (int) decimalPoint),
      emissionAction
    };
  }

  @Override
  public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {
    return null;
  }
}
