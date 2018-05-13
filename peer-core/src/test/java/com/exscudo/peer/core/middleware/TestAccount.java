package com.exscudo.peer.core.middleware;

import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.AccountProperty;
import com.exscudo.peer.core.data.identifier.AccountID;

// ATTENTION: side effects
public class TestAccount extends Account {
    public TestAccount(AccountID id) {
        super(id);
    }

    @Override
    public TestAccount putProperty(AccountProperty prop) {
        properties.put(prop.getType(), prop);
        return this;
    }

    @Override
    public TestAccount removeProperty(String id) {
        properties.remove(id);
        return this;
    }
}
