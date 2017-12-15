package com.exscudo.eon.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.store.sqlite.ConnectionProxy;
import com.exscudo.peer.store.sqlite.Storage;
import com.exscudo.peer.store.sqlite.utils.TransactionHelper;

public class TransactionHistoryHelper {
	private final static int PAGE_SIZE = 20;

	public static long getAccountId(String accountId) throws RemotePeerException {
		try {
			return Format.ID.accountId(accountId);
		} catch (IllegalArgumentException e) {
			throw new RemotePeerException(e);
		}
	}

	public static List<Transaction> getCommittedPage(Storage storage, String accountId, int page)
			throws RemotePeerException {
		long accID = getAccountId(accountId);
		ConnectionProxy connection = storage.getConnection();
		return TransactionHelper.findByAccount(connection, accID, page * PAGE_SIZE, PAGE_SIZE);
	}

	public static List<Transaction> getUncommitted(Storage storage, String accountId) throws RemotePeerException {
		List<Transaction> items = new ArrayList<>();

		long accID = getAccountId(accountId);

		IBacklogService backlog = storage.getBacklog();
		final Iterator<Long> indexes = backlog.iterator();

		while (indexes.hasNext()) {
			long item = indexes.next();
			Transaction transaction = backlog.get(item);
			if (transaction != null) {
				if (transaction.getSenderID() == accID) {
					items.add(transaction);
				} else if (transaction.getData() != null && accountId.equals(transaction.getData().get("recipient"))) {
					items.add(transaction);
				}
			}
		}

		return items;
	}
}
