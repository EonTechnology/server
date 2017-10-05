package com.exscudo.peer.store.sqlite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.TransactionComparator;
import com.exscudo.peer.core.exceptions.DataAccessException;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.store.sqlite.lock.ILockableObject;

/**
 * Basic implementation of {@code IBacklogService}.
 * <p>
 * Use {@link #createSavepoint} and {@link #loadSavepoint} for serialization of
 * current state.
 *
 * @see IBacklogService
 */
public class Backlog implements ILockableObject, IBacklogService {
	private final Object syncRoot = new Object();
	private volatile ConcurrentHashMap<Long, Transaction> transactions = new ConcurrentHashMap<>();
	private volatile ConcurrentSkipListSet<Long> keys = new ConcurrentSkipListSet<>(new LongComparator(transactions));

	@Override
	public Object getIdentifier() {
		return syncRoot;
	}

	@Override
	public synchronized boolean put(Transaction transaction) {
		long id = transaction.getID();
		transactions.put(id, transaction);
		keys.add(id);
		return true;
	}

	@Override
	public synchronized Transaction remove(long id) {
		if (transactions.containsKey(id)) {
			keys.remove(id);
			return transactions.remove(id);
		}
		return null;
	}

	@Override
	public Transaction get(long id) {
		return transactions.get(id);
	}

	@Override
	public boolean contains(long id) {
		return transactions.containsKey(id);
	}

	@Override
	public Iterator<Long> iterator() {
		return keys.iterator();
	}

	public int size() {
		return transactions.size();
	}

	synchronized Savepoint createSavepoint() {

		try {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				try (ObjectOutputStream ous = new ObjectOutputStream(baos)) {
					ous.writeObject(transactions);
					return new Savepoint(baos.toByteArray());
				}
			}
		} catch (Exception e) {
			throw new DataAccessException(e);
		}

	}

	@SuppressWarnings("unchecked")
	synchronized void loadSavepoint(Savepoint pt) {

		try {

			ConcurrentHashMap<Long, Transaction> transactionSet;
			try (ByteArrayInputStream bais = new ByteArrayInputStream(pt.data)) {
				try (ObjectInputStream ois = new ObjectInputStream(bais)) {
					transactionSet = (ConcurrentHashMap<Long, Transaction>) ois.readObject();
				}
			}

			ConcurrentSkipListSet<Long> listSet = new ConcurrentSkipListSet<>(new LongComparator(transactionSet));
			listSet.addAll(transactionSet.keySet());

			this.transactions = transactionSet;
			this.keys = listSet;
		} catch (Exception e) {
			throw new DataAccessException(e);
		}

	}

	static class Savepoint {
		public final byte[] data;

		public Savepoint(byte[] data) {
			this.data = data;
		}
	}

	private static class LongComparator implements Comparator<Long> {

		private volatile ConcurrentHashMap<Long, Transaction> transactions;

		public LongComparator(ConcurrentHashMap<Long, Transaction> transactions) {

			this.transactions = transactions;
		}

		private transient Comparator<Transaction> comparatorTran = new TransactionComparator();
		@Override
		public int compare(Long a, Long b) {
			Transaction aTx = this.transactions.get(a);
			Transaction bTx = this.transactions.get(b);
			return comparatorTran.compare(aTx, bTx);
		}
	}

}
