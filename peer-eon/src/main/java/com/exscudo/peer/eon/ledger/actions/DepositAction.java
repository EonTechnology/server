package com.exscudo.peer.eon.ledger.actions;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.GeneratingBalanceProperty;

public class DepositAction implements ILedgerAction {
    private final AccountID accountID;
    private final long amount;

    public DepositAction(AccountID accountID, long amount) {
        this.accountID = accountID;
        this.amount = amount;
    }

    private ValidationResult canRun(ILedger ledger) {
        Account account = ledger.getAccount(accountID);
        if (account == null) {
            return ValidationResult.error("Unknown sender.");
        }

        BalanceProperty balanceProperty = AccountProperties.getBalance(account);
        GeneratingBalanceProperty depositProperty = AccountProperties.getDeposit(account);

        if ((balanceProperty.getValue() + depositProperty.getValue()) < amount) {
            return ValidationResult.error("Not enough funds.");
        }

        if (depositProperty.getValue() == amount) {
            return ValidationResult.error("Value already set.");
        }

        return ValidationResult.success;
    }

    @Override
    public ILedger run(ILedger ledger, TransactionContext context) throws ValidateException {
        ValidationResult r = canRun(ledger);
        if (r.hasError) {
            throw r.cause;
        }

        Account account = ledger.getAccount(accountID);

        BalanceProperty balanceProperty = AccountProperties.getBalance(account);
        GeneratingBalanceProperty depositProperty = AccountProperties.getDeposit(account);

        long balance = balanceProperty.getValue() + depositProperty.getValue() - amount;

        depositProperty.setValue(amount).setTimestamp(context.getTimestamp());
        balanceProperty.setValue(balance);

        account = AccountProperties.setProperty(account, balanceProperty);
        account = AccountProperties.setProperty(account, depositProperty);

        ILedger newLedger = ledger.putAccount(account);

        return newLedger;
    }
}
