package com.exscudo.peer.eon.tx.rules;

import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.core.data.identifier.AccountID;

// TODO: remove side effects
public class DefaultAccount extends Account {
    public DefaultAccount(AccountID id) {
        super(id);
    }

    @Override
    public DefaultAccount putProperty(AccountProperty prop) {
        properties.put(prop.getType(), prop);
        return this;
    }

    @Override
    public DefaultAccount removeProperty(String id) {
        properties.remove(id);
        return this;
    }
}
