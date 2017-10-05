package com.exscudo.eon.bot;

import java.io.IOException;
import java.util.Iterator;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.transactions.utils.AccountBalance;
import com.exscudo.peer.eon.transactions.utils.AccountDeposit;
import com.exscudo.peer.store.sqlite.LedgerState;
import com.exscudo.peer.store.sqlite.Storage;

/**
 * Account status service.
 */
public class AccountService {

	/**
	 * Account status
	 */
	public static class State {

		/**
		 * Account does not exist
		 */
		public static final State NotFound = new State(404, "Not Found");

		/**
		 * Account is in processing
		 */
		public static final State Processing = new State(102, "Processing");

		/**
		 * Account is registered
		 */
		public static final State OK = new State(200, "OK");

		public final int code;
		public final String name;

		private State(int code, String name) {
			this.code = code;
			this.name = name;
		}
	}

	/**
	 * Account state
	 */
	public static class Info {
		public State state;
		public long amount;
		public long deposit;
	}

	private final Storage storage;

	public AccountService(Storage storage) {
		this.storage = storage;
	}

	/**
	 * Get account status
	 * 
	 * @param id
	 *            account ID
	 * @return
	 * @throws RemotePeerException
	 * @throws IOException
	 */
	public State getState(String id) throws RemotePeerException, IOException {

		long accID;
		try {
			accID = Format.ID.accountId(id);
		} catch (IllegalArgumentException e) {
			throw new RemotePeerException(e);
		}

		final ILedger ledgerState = new LedgerState(storage, storage.getLastBlock());
		final boolean exist = ledgerState.existAccount(accID);

		if (exist) {
			return State.OK;
		}

		IBacklogService backlog = storage.getBacklog();
		final Iterator<Long> indexes = backlog.iterator();
		while (indexes.hasNext()) {
			long item = indexes.next();
			Transaction transaction = backlog.get(item);

			if (transaction != null && transaction.getType() == TransactionType.AccountRegistration) {

				if (transaction.getData().keySet().contains(id)) {
					return State.Processing;
				}

			}

		}

		return State.NotFound;
	}

	/**
	 * Get account state
	 * 
	 * @param id
	 *            account ID
	 * @return
	 * @throws RemotePeerException
	 * @throws IOException
	 */
	public Info getInformation(String id) throws RemotePeerException, IOException {

		final ILedger ledgerState = new LedgerState(storage, storage.getLastBlock());

		long accID;
		try {
			accID = Format.ID.accountId(id);
		} catch (IllegalArgumentException e) {
			throw new RemotePeerException(e);
		}

		IAccount account = ledgerState.getAccount(accID);
		Info info = new Info();
		info.state = getState(id);

		if (account != null) {
			info.amount = AccountBalance.getBalance(account);

			// TODO: add current generating balance
			info.deposit = AccountDeposit.getDeposit(account);
		}

		return info;
	}
}
