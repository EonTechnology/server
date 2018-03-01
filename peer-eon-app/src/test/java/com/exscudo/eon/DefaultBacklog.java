package com.exscudo.eon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.exscudo.peer.core.backlog.IBacklogService;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.TransactionID;

public class DefaultBacklog implements IBacklogService {
    ConcurrentHashMap<TransactionID, Transaction> map = new ConcurrentHashMap<>();

    @Override
    public boolean put(Transaction transaction) throws ValidateException {
        map.put(transaction.getID(), transaction);
        return true;
    }

    @Override
    public Transaction get(TransactionID id) {
        return map.get(id);
    }

    @Override
    public Iterator<TransactionID> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public synchronized List<Transaction> copyAndClear() {
        ArrayList<Transaction> copy = new ArrayList<>(map.values());
        map.clear();
        return copy;
    }

    @Override
    public int size() {
        return map.size();
    }
}
