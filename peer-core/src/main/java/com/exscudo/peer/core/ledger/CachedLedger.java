package com.exscudo.peer.core.ledger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.exscudo.peer.core.common.CachedHashMap;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;

public class CachedLedger extends AbstractLedger {

    protected final Map<AccountID, Account> writeCache;
    protected final Map<AccountID, Account> readCache;
    protected ILedger base;

    public CachedLedger(ILedger ledger) {
        writeCache = Collections.synchronizedMap(new HashMap<>());
        readCache = Collections.synchronizedMap(new CachedHashMap<>(1000));
        base = ledger;
    }

    private CachedLedger(ILedger ledger, Map<AccountID, Account> writeMap, Map<AccountID, Account> readMap) {
        writeCache = writeMap;
        readCache = readMap;
        base = ledger;
    }

    @Override
    public Account getAccount(AccountID accountID) {
        Account account = writeCache.get(accountID);
        if (account == null) {
            account = readCache.get(accountID);
        }
        if (account == null) {
            account = base.getAccount(accountID);
            readCache.put(accountID, account);
        }
        return account;
    }

    @Override
    public ILedger putAccount(Account account) {
        Map<AccountID, Account> map = Collections.synchronizedMap(new HashMap<>(writeCache));
        map.put(account.getID(), account);
        return new CachedLedger(base, map, readCache);
    }

    @Override
    public String getHash() {
        updateBase();
        return base.getHash();
    }

    @Override
    public void save() {
        updateBase();
        base.save();
    }

    @Override
    public Iterator<Account> iterator() {
        updateBase();
        return base.iterator();
    }

    @Override
    public Iterator<Account> iterator(AccountID fromAcc) {
        return base.iterator(fromAcc);
    }

    private void updateBase() {
        for (Map.Entry<AccountID, Account> entry : writeCache.entrySet()) {
            base = base.putAccount(entry.getValue());
        }
    }
}
