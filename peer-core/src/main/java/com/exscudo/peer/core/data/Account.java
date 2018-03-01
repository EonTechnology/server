package com.exscudo.peer.core.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.data.identifier.AccountID;

/**
 * Basic implementation of Account.
 * <p>
 * An account is treated as a set of properties that are changed through
 * transactions. The property specification is defined in the transaction type.
 */
public class Account {
    protected final AccountID id;
    protected final Map<String, AccountProperty> properties;

    public Account(AccountID id) {
        this.id = id;
        this.properties = new HashMap<>();
    }

    public Account(AccountID id, AccountProperty[] props) {
        this.id = id;
        this.properties = new HashMap<>();
        for (AccountProperty p : props) {
            this.properties.put(p.getType(), p);
        }
    }

    private Account(AccountID id, Map<String, AccountProperty> properties) {
        this.id = id;
        this.properties = properties;
    }

    public Account(Account account) {

        this(account.getID(), account.getProperties().toArray(new AccountProperty[0]));
    }

    /**
     * Returns the account identifier.
     *
     * @return
     */
    public AccountID getID() {
        return id;
    }

    /**
     * Returns the property defined by {@code id} or null.
     *
     * @param id One of the well-known types
     * @return
     */
    public AccountProperty getProperty(String id) {
        return properties.get(id);
    }

    /**
     * Adds a given {@code property}. If the property already exists, then its will
     * be updated.
     *
     * @param prop
     * @return new account
     */
    public Account putProperty(AccountProperty prop) {
        HashMap<String, AccountProperty> newProperties = new HashMap<>(properties);
        newProperties.put(prop.getType(), prop);

        return new Account(getID(), newProperties);
    }

    /**
     * Returns a list of all properties.
     *
     * @return
     */
    public Collection<AccountProperty> getProperties() {
        return properties.values();
    }

    /**
     * Removes a property defined by {@code id}
     *
     * @param id
     * @return new account
     */
    public Account removeProperty(String id) {
        HashMap<String, AccountProperty> newProperties = new HashMap<>(properties);
        newProperties.remove(id);

        return new Account(getID(), newProperties);
    }
}
