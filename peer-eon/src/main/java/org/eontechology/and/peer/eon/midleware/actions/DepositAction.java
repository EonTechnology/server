package org.eontechology.and.peer.eon.midleware.actions;

import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.middleware.ILedgerAction;
import org.eontechology.and.peer.core.middleware.LedgerActionContext;
import org.eontechology.and.peer.eon.ledger.AccountProperties;
import org.eontechology.and.peer.eon.ledger.state.BalanceProperty;
import org.eontechology.and.peer.eon.ledger.state.GeneratingBalanceProperty;
import org.eontechology.and.peer.eon.midleware.Resources;

public class DepositAction implements ILedgerAction {
    private final AccountID accountID;
    private final long amount;

    public DepositAction(AccountID accountID, long amount) {
        this.accountID = accountID;
        this.amount = amount;
    }

    private void ensureValidState(ILedger ledger) throws ValidateException {

        Account account = ledger.getAccount(accountID);
        if (account == null) {
            throw new ValidateException(Resources.SENDER_ACCOUNT_NOT_FOUND);
        }

        BalanceProperty balanceProperty = AccountProperties.getBalance(account);
        GeneratingBalanceProperty depositProperty = AccountProperties.getDeposit(account);

        if ((balanceProperty.getValue() + depositProperty.getValue()) < amount) {
            throw new ValidateException(Resources.NOT_ENOUGH_FUNDS);
        }

        if (depositProperty.getValue() == amount) {
            throw new ValidateException(Resources.VALUE_ALREADY_SET);
        }
    }

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

        ensureValidState(ledger);

        Account account = ledger.getAccount(accountID);

        BalanceProperty balanceProperty = AccountProperties.getBalance(account);
        GeneratingBalanceProperty depositProperty = AccountProperties.getDeposit(account);

        long balance = balanceProperty.getValue() + depositProperty.getValue() - amount;

        depositProperty.setValue(amount).setTimestamp(context.getTimestamp());
        balanceProperty.setValue(balance);

        account = AccountProperties.setProperty(account, balanceProperty);
        account = AccountProperties.setProperty(account, depositProperty);

        return ledger.putAccount(account);
    }
}
