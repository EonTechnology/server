package org.eontechnology.and.peer.eon.midleware.actions;

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

public class RejectionAction implements ILedgerAction {
  private final AccountID accountID;
  private final AccountID delegateID;

  public RejectionAction(AccountID delegateID, AccountID accountID) {
    this.accountID = accountID;
    this.delegateID = delegateID;
  }

  private void ensureValidState(ILedger ledger) throws ValidateException {
    Account delegate = ledger.getAccount(delegateID);
    if (delegate == null) {
      throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
    }

    Account account = ledger.getAccount(accountID);
    if (account == null) {
      throw new ValidateException(Resources.TARGET_ACCOUNT_NOT_FOUND);
    }

    ValidationModeProperty validationMode = AccountProperties.getValidationMode(account);
    if (!validationMode.containWeightForAccount(delegate.getID())) {
      throw new ValidateException(Resources.ACCOUNT_NOT_IN_VOTE_POLL);
    }
    if (validationMode.getMaxWeight() == validationMode.getWeightForAccount(delegate.getID())) {
      throw new ValidateException(Resources.REJECTION_NOT_POSSIBLE);
    }
  }

  @Override
  public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

    ensureValidState(ledger);

    ILedger newLedger = ledger;

    // Update sender account
    Account sender = newLedger.getAccount(delegateID);
    VotePollsProperty senderVoter = AccountProperties.getVoter(sender);

    senderVoter.setPoll(accountID, 0);
    senderVoter.setTimestamp(context.getTimestamp());
    sender = AccountProperties.setProperty(sender, senderVoter);
    newLedger = newLedger.putAccount(sender);

    // Update target account
    Account target = newLedger.getAccount(accountID);
    ValidationModeProperty targetValidationMode = AccountProperties.getValidationMode(target);
    targetValidationMode.setWeightForAccount(delegateID, 0);
    targetValidationMode.setTimestamp(context.getTimestamp());
    target = AccountProperties.setProperty(target, targetValidationMode);
    newLedger = newLedger.putAccount(target);

    return newLedger;
  }
}
