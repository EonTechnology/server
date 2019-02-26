package org.eontechology.and.peer.eon.midleware.actions;

import java.util.ArrayList;

import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.ledger.ILedger;
import org.eontechology.and.peer.core.middleware.ILedgerAction;
import org.eontechology.and.peer.core.middleware.LedgerActionContext;
import org.eontechology.and.peer.eon.ledger.AccountProperties;
import org.eontechology.and.peer.eon.ledger.state.RegistrationDataProperty;
import org.eontechology.and.peer.eon.midleware.Resources;

public class RegistrationAction implements ILedgerAction {
    private final ArrayList<Item> newAccounts = new ArrayList<>();

    public void addAccount(AccountID id, byte[] publicKey) {
        newAccounts.add(new Item(id, publicKey));
    }

    private void ensureValidState(ILedger ledger) throws ValidateException {

        for (Item item : newAccounts) {
            Account account = ledger.getAccount(item.accountID);
            if (account != null) {
                throw new ValidateException(Resources.ACCOUNT_ALREADY_EXISTS);
            }
        }
    }

    @Override
    public ILedger run(ILedger ledger, LedgerActionContext context) throws ValidateException {

        ensureValidState(ledger);

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
