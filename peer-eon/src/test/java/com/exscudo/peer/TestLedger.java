package com.exscudo.peer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.ledger.ILedger;

// ATTENTION: side effects
public class TestLedger implements ILedger {
    private Map<AccountID, Account> accounts = new HashMap<>();

    @Override
    public Account getAccount(AccountID accountID) {
        return accounts.get(accountID);
    }

    @Override
    public ILedger putAccount(Account account) {
        accounts.put(account.getID(), account);
        return this;
    }

    @Override
    public String getHash() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save() {
    }

    @Override
    public Iterator<Account> iterator() {
        return accounts.values().iterator();
    }
}
