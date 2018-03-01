package com.exscudo.peer.core.ledger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.ledger.tree.CachedNodeCollection;
import com.exscudo.peer.core.ledger.tree.IValueConverter;
import com.exscudo.peer.core.ledger.tree.StateTree;
import com.j256.ormlite.support.ConnectionSource;

/**
 * Implementation of {@code ILedger} interface using by Merkle Tree.
 * <p>
 * Instead of returning IAccount in {@link ILedger#getAccount}
 *
 * @see ILedger
 * @see Account
 */
public class Ledger implements ILedger {
    private final StateTree<Account> stateTree;
    private final int timestamp;
    private StateTree.State<Account> state;

    public Ledger(ConnectionSource connectionSource, String snapshot, int timestamp) {

        ValueConverter vc = new ValueConverter();
        this.stateTree = new StateTree<>(new CachedNodeCollection(connectionSource), vc);
        this.state = null;
        this.timestamp = timestamp;

        if (snapshot != null) {
            this.state = stateTree.getState(snapshot);
        }
    }

    private Ledger(StateTree<Account> stateTree, StateTree.State<Account> state, int timestamp) {
        this.stateTree = stateTree;
        this.state = state;
        this.timestamp = timestamp;
    }

    @Override
    public Account getAccount(AccountID accountID) {
        return state.get(accountID.getValue());
    }

    @Override
    public Ledger putAccount(Account account) {
        StateTree.State newState = stateTree.newState(state, account.getID().getValue(), account, timestamp);
        return new Ledger(stateTree, newState, timestamp);
    }

    @Override
    public String getHash() {
        return state.getName();
    }

    public void save() {
        if (state == null) {
            return;
        }
        stateTree.saveState(state);
    }

    @Override
    public Iterator<Account> iterator() {
        return state.iterator();
    }

    private static class ValueConverter implements IValueConverter<Account> {

        @Override
        public Account convert(Map<String, Object> map) {
            ArrayList<AccountProperty> properties = new ArrayList<>();
            if (map.containsKey("properties")) {
                Map<String, Object> p = (Map<String, Object>) map.get("properties");

                for (Map.Entry<String, Object> o : p.entrySet()) {

                    AccountProperty property =
                            new AccountProperty(String.valueOf(o.getKey()), (Map<String, Object>) o.getValue());
                    properties.add(property);
                }

                return new Account(new AccountID(map.get("id").toString()), properties.toArray(new AccountProperty[0]));
            }
            return null;
        }

        @Override
        public Map<String, Object> convert(Account value) {
            Map<String, Object> properties = new TreeMap<>();
            for (AccountProperty property : value.getProperties()) {
                properties.put(property.getType(), property.getData());
            }
            Map<String, Object> map = new TreeMap<>();
            map.put("id", value.getID().toString());
            map.put("properties", properties);
            return map;
        }
    }
}
