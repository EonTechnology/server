package com.exscudo.peer.eon.ledger.actions;

import java.util.ArrayList;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.ILedgerAction;
import com.exscudo.peer.eon.ledger.state.RegistrationDataProperty;

public class RegistrationAction implements ILedgerAction {
    private final ArrayList<Item> newAccounts = new ArrayList<>();

    public void addAccount(AccountID id, byte[] publicKey) {
        newAccounts.add(new Item(id, publicKey));
    }

    private ValidationResult canRun(ILedger ledger) {

        for (Item item : newAccounts) {
            Account account = ledger.getAccount(item.accountID);
            if (account != null) {
                return ValidationResult.error("Account already exists.");
            }
        }
        return ValidationResult.success;
    }

    @Override
    public ILedger run(ILedger ledger, TransactionContext context) throws ValidateException {
        ValidationResult r = canRun(ledger);
        if (r.hasError) {
            throw r.cause;
        }

        ILedger newLedger = ledger;
        for (Item newAccount : newAccounts) {
            Account account = new Account(newAccount.accountID);
            RegistrationDataProperty registrationDataProperty = new RegistrationDataProperty(newAccount.publicKey);
            account = AccountProperties.setProperty(account, registrationDataProperty);
            newLedger = newLedger.putAccount(account);
        }

        return newLedger;
    }

    static class Item {
        public final AccountID accountID;
        public final byte[] publicKey;

        public Item(AccountID accountID, byte[] publicKey) {
            this.accountID = accountID;
            this.publicKey = publicKey;
        }
    }
}
