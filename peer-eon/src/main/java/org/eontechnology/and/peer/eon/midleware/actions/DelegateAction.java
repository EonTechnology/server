package org.eontechnology.and.peer.eon.midleware.actions;

import java.util.ArrayList;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.LedgerActionContext;
import org.eontechnology.and.peer.eon.ledger.AccountProperties;
import org.eontechnology.and.peer.eon.ledger.state.ValidationModeProperty;
import org.eontechnology.and.peer.eon.ledger.state.VotePollsProperty;
import org.eontechnology.and.peer.eon.midleware.Resources;

public class DelegateAction implements ILedgerAction {

  private final ArrayList<Item> delegates = new ArrayList<>();
  private final AccountID accountID;

  public DelegateAction(AccountID accountID) {
    this.accountID = accountID;
  }

  public void addDelegate(AccountID id, int weight) {
    delegates.add(new Item(id, weight));
  }

  private void ensureValidState(ILedger ledger) throws ValidateException {

    Account sender = ledger.getAccount(accountID);
    if (sender == null) {
      throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
    }

    ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);

    int delegatesCount = validationMode.delegatesEntrySet().size();
    int deltaWeight = 0;
    for (Item di : delegates) {

      if (di.id.equals(accountID)) {

        // cancellation of rights is not possible
        if (validationMode.isPublic()) {
          throw new ValidateException(Resources.PUBLIC_ACCOUNT_PROHIBITED_ACTION);
        }

        if (di.weight == validationMode.getBaseWeight()) {
          throw new ValidateException(Resources.VALUE_ALREADY_SET);
        }

        deltaWeight += di.weight - validationMode.getBaseWeight();
      } else {

        Account account = ledger.getAccount(di.id);
        if (account == null) {
          throw new ValidateException(Resources.DELEGATE_ACCOUNT_NOT_FOUND);
        }
        ValidationModeProperty accountValidationMode = AccountProperties.getValidationMode(account);
        if (accountValidationMode.isPublic()) {
          throw new ValidateException(Resources.PUBLIC_ACCOUNT_PROHIBITED_ACTION);
        }

        if (di.weight != 0) {
          VotePollsProperty accountVotePolls = AccountProperties.getVoter(account);
          if (accountVotePolls.isFull() && !accountVotePolls.contains(sender.getID())) {
            throw new ValidateException(Resources.TOO_MACH_SIZE);
          }
        }

        if (validationMode.containWeightForAccount(di.id)) {
          int oldWeight = validationMode.getWeightForAccount(di.id);

          if (di.weight == oldWeight) {
            throw new ValidateException(Resources.VALUE_ALREADY_SET);
          }
          if (di.weight == 0) {
            delegatesCount--;
          }

          deltaWeight += di.weight - oldWeight;
        } else {
          if (di.weight == 0) {
            throw new ValidateException(Resources.VALUE_ALREADY_SET);
          }

          delegatesCount++;
          deltaWeight += di.weight;
        }
      }
    }

    if ((validationMode.getMaxWeight() + deltaWeight) < ValidationModeProperty.MAX_QUORUM) {
      throw new ValidateException(Resources.VOTES_INCORRECT_DISTRIBUTION);
    }

    if (delegatesCount > Constant.TRANSACTION_CONFIRMATIONS_MAX_SIZE) {
      throw new ValidateException(Resources.TOO_MACH_SIZE);
    }
  }

  @Override
  public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

    ensureValidState(ledger);

    ILedger newLedger = ledger;

    Account sender = newLedger.getAccount(accountID);
    ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);

    for (Item delegate : delegates) {

      if (delegate.id.equals(sender.getID())) {
        validationMode.setBaseWeight(delegate.weight);
      } else {

        Account target = newLedger.getAccount(delegate.id);
        VotePollsProperty targetVoter = AccountProperties.getVoter(target);

        targetVoter.setPoll(accountID, delegate.weight);
        targetVoter.setTimestamp(context.getTimestamp());
        target = AccountProperties.setProperty(target, targetVoter);
        newLedger = newLedger.putAccount(target);

        validationMode.setWeightForAccount(delegate.id, delegate.weight);
      }
    }

    validationMode.setTimestamp(context.getTimestamp());
    sender = AccountProperties.setProperty(sender, validationMode);
    newLedger = newLedger.putAccount(sender);

    return newLedger;
  }

  static class Item {
    public final AccountID id;
    public final int weight;

    public Item(AccountID id, int weight) {
      this.id = id;
      this.weight = weight;
    }
  }
}
