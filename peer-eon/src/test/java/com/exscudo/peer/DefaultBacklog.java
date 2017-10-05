package com.exscudo.peer;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.services.IBacklogService;

public class DefaultBacklog implements IBacklogService {
	ConcurrentHashMap<Long, Transaction> map = new ConcurrentHashMap<>();

	@Override
	public boolean put(Transaction transaction) throws ValidateException {
		map.put(transaction.getID(), transaction);
		return true;
	}

	@Override
	public Transaction remove(long id) {
		return map.remove(id);
	}

	@Override
	public Transaction get(long id) {
		return map.get(id);
	}

	@Override
	public Iterator<Long> iterator() {
		return map.keySet().iterator();
	}
}
