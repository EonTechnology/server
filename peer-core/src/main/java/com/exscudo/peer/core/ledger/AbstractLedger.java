package com.exscudo.peer.core.ledger;

import java.util.Iterator;

import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.AccountID;

/**
 * Abstract implementation of {@code ILedger} interface.
 */
public abstract class AbstractLedger implements ILedger {
    @Override
    public Iterator<Account> iterator(AccountID fromAcc) {
        Iterator<Account> iterator = this.iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()) {
            Account next = iterator.next();
            if (next.getID().equals(fromAcc)) {
                return iterator;
            }
        }
        return null;
    }
}
