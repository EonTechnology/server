package org.eontechnology.and.peer.core.middleware;

import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.AccountProperty;
import org.eontechnology.and.peer.core.data.identifier.AccountID;

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
