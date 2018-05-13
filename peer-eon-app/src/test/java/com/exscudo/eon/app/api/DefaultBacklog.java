package com.exscudo.eon.app.api;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.exscudo.peer.core.backlog.IBacklog;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.TransactionID;

public class DefaultBacklog implements IBacklog {
    ConcurrentHashMap<TransactionID, Transaction> map = new ConcurrentHashMap<>();

    @Override
    public synchronized void put(Transaction transaction) throws ValidateException {
        map.put(transaction.getID(), transaction);
    }

    @Override
    public Transaction get(TransactionID id) {
        return map.get(id);
    }

    @Override
    public Iterator<TransactionID> iterator() {
        return map.keySet().iterator();
    }
}
