package org.eontechology.and.peer.core.ledger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import org.eontechology.and.peer.core.common.CachedHashMap;
import org.eontechology.and.peer.core.data.Account;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.ledger.tree.StateTree;

/**
 * Abstract implementation of {@code ILedger} interface.
 * <p>
 * Instead of returning IAccount in {@link ILedger#getAccount}
 *
 * @see ILedger
 * @see Account
 */
public class Ledger implements ILedger {

    private final Map<AccountID, Account> writeCache;
    private final Map<AccountID, Account> readCache;
    private final Function<StateTree<Account>, Void> saveCallback;
    private StateTree<Account> state;

    public Ledger(StateTree<Account> state, Function<StateTree<Account>, Void> saveCallback) {
        this(state,
             saveCallback,
             Collections.synchronizedMap(new HashMap<>()),
             Collections.synchronizedMap(new CachedHashMap<>(1000)));
    }

    private Ledger(StateTree<Account> state,
                   Function<StateTree<Account>, Void> saveCallback,
                   Map<AccountID, Account> writeMap,
                   Map<AccountID, Account> readMap) {
        this.writeCache = writeMap;
        this.readCache = readMap;
        this.state = state;
        this.saveCallback = saveCallback;
    }

    @Override
    public Account getAccount(AccountID accountID) {
        Account account = writeCache.get(accountID);
        if (account == null) {
            account = readCache.get(accountID);
        }
        if (account == null) {
            if (state != null) {
                account = state.get(accountID.getValue());
            }
            readCache.put(accountID, account);
        }
        return account;
    }

    @Override
    public Ledger putAccount(Account account) {
        Map<AccountID, Account> map = Collections.synchronizedMap(new HashMap<>(writeCache));
        map.put(account.getID(), account);
        return new Ledger(state, saveCallback, map, readCache);
    }

    @Override
    public String getHash() {
        updateBase();
        return state.getName();
    }

    @Override
    public synchronized void save() {
        updateBase();
        saveCallback.apply(state);
    }

    @Override
    public Iterator<Account> iterator() {
        updateBase();
        return state.iterator();
    }

    private synchronized void updateBase() {
        if (!writeCache.isEmpty()) {

            StateTree<Account> newState = StateTree.createNew(state, writeCache.values().iterator());

            readCache.putAll(writeCache);
            writeCache.clear();

            this.state = newState;
        }
    }
}
