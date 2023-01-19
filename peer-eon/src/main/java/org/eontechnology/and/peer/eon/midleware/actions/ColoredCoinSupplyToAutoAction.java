package org.eontechnology.and.peer.eon.midleware.actions;

import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.LedgerActionContext;
import org.eontechnology.and.peer.eon.ledger.AccountProperties;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinEmitMode;
import org.eontechnology.and.peer.eon.ledger.state.ColoredCoinProperty;
import org.eontechnology.and.peer.eon.midleware.Resources;

public class ColoredCoinSupplyToAutoAction implements ILedgerAction {

  private final AccountID accountID;

  public ColoredCoinSupplyToAutoAction(AccountID accountID) {
    this.accountID = accountID;
  }

  @Override
  public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

    Account account = ledger.getAccount(accountID);

    if (account == null) {
      throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
    }

    ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(account);
    if (!coloredCoin.isIssued()) {
      throw new ValidateException(Resources.COLORED_COIN_NOT_EXISTS);
    }

    if (coloredCoin.getEmitMode() == ColoredCoinEmitMode.AUTO) {
      throw new ValidateException(Resources.VALUE_ALREADY_SET);
    }

    coloredCoin.setEmitMode(ColoredCoinEmitMode.AUTO);
    account = AccountProperties.setProperty(account, coloredCoin);

    return ledger.putAccount(account);
  }
}
