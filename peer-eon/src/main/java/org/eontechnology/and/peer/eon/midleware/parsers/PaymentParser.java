package org.eontechnology.and.peer.eon.midleware.parsers;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.eon.midleware.Resources;
import org.eontechnology.and.peer.eon.midleware.actions.FeePaymentAction;
import org.eontechnology.and.peer.eon.midleware.actions.PaymentAction;

public class PaymentParser implements ITransactionParser {

  @Override
  public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

    if (transaction.getNestedTransactions() != null) {
      throw new ValidateException(Resources.NESTED_TRANSACTION_NOT_SUPPORTED);
    }

    Map<String, Object> data = transaction.getData();
    if (data.size() != 2) {
      throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
    }

    if (!(data.get("amount") instanceof Long)) {
      throw new ValidateException(Resources.AMOUNT_INVALID_FORMAT);
    }

    long amount = (long) data.get("amount");
    if (amount <= 0) {
      throw new ValidateException(Resources.AMOUNT_OUT_OF_RANGE);
    }

    AccountID recipientID;
    try {
      recipientID = new AccountID(String.valueOf(data.get("recipient")));
    } catch (Exception e) {
      throw new ValidateException(Resources.RECIPIENT_INVALID_FORMAT);
    }

    return new ILedgerAction[] {
      new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()),
      new PaymentAction(transaction.getSenderID(), amount, recipientID)
    };
  }

  @Override
  public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {

    AccountID recipientID;
    try {
      recipientID = new AccountID(transaction.getData().get("recipient").toString());
    } catch (Exception e) {
      throw new ValidateException(Resources.RECIPIENT_INVALID_FORMAT);
    }
    return Collections.singleton(recipientID);
  }
}
