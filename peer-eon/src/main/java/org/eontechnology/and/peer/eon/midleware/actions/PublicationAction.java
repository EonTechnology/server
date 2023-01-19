package org.eontechnology.and.peer.eon.midleware.actions;

import java.util.Arrays;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.crypto.ISignature;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.LedgerActionContext;
import org.eontechnology.and.peer.eon.ledger.AccountProperties;
import org.eontechnology.and.peer.eon.ledger.state.RegistrationDataProperty;
import org.eontechnology.and.peer.eon.ledger.state.ValidationModeProperty;
import org.eontechnology.and.peer.eon.ledger.state.VotePollsProperty;
import org.eontechnology.and.peer.eon.midleware.Resources;

public class PublicationAction implements ILedgerAction {
  private final AccountID accountID;
  private final String seed;
  private final ISignature signature;

  public PublicationAction(AccountID accountID, String seed, ISignature signature) {
    this.accountID = accountID;
    this.seed = seed;
    this.signature = signature;
  }

  private void ensureValidState(ILedger ledger, LedgerActionContext context)
      throws ValidateException {
    Account sender = ledger.getAccount(accountID);
    if (sender == null) {
      throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
    }

    byte[] publicKey;
    try {
      publicKey = signature.getKeyPair(Format.convert(seed)).publicKey;
    } catch (Exception e) {
      throw new ValidateException(Resources.PUBLIC_ACCOUNT_INVALID_SEED);
    }

    RegistrationDataProperty registration = AccountProperties.getRegistration(sender);
    if (!Arrays.equals(registration.getPublicKey(), publicKey)) {
      throw new ValidateException(Resources.PUBLIC_ACCOUNT_SEED_NOT_MATCH);
    }

    ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);
    if (validationMode.isPublic()) {
      throw new ValidateException(Resources.VALUE_ALREADY_SET);
    }
    if (validationMode.getBaseWeight() != ValidationModeProperty.MIN_WEIGHT
        || validationMode.getMaxWeight() == validationMode.getBaseWeight()) {
      throw new ValidateException(Resources.PUBLIC_ACCOUNT_INVALID_WEIGHT);
    }

    VotePollsProperty voter = AccountProperties.getVoter(sender);
    if (voter.hasPolls()) {
      throw new ValidateException(Resources.PUBLIC_ACCOUNT_PARTICIPATES_IN_VOTE_POLLS);
    }

    int timestamp = context.getTimestamp() - Constant.SECONDS_IN_DAY;
    if (validationMode.getTimestamp() > timestamp || voter.getTimestamp() > timestamp) {
      throw new ValidateException(Resources.PUBLIC_ACCOUNT_RECENTLY_CHANGED);
    }
  }

  @Override
  public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

    ensureValidState(ledger, context);

    Account sender = ledger.getAccount(accountID);
    ValidationModeProperty validationMode = AccountProperties.getValidationMode(sender);
    validationMode.setPublicMode(seed);
    validationMode.setTimestamp(context.getTimestamp());

    sender = AccountProperties.setProperty(sender, validationMode);

    return ledger.putAccount(sender);
  }
}
