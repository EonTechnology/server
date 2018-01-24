package com.exscudo.eon.bot;

import java.io.IOException;

import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.eon.state.ColoredCoin;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import com.exscudo.peer.eon.utils.ColoredCoinId;
import com.exscudo.peer.store.sqlite.Storage;
import com.exscudo.peer.store.sqlite.merkle.Ledgers;

/**
 * Colored coin service.
 */
public class ColoredCoinService {

	public static class State {

		/**
		 * Colored coin is registered
		 */
		public static final State OK = new State(200, "OK");

		/**
		 * No associated colored coins
		 */
		public static final State Unauthorized = new State(401, "Unauthorized");

		public final int code;
		public final String name;

		private State(int code, String name) {
			this.code = code;
			this.name = name;
		}
	}

	public static class Info {
		public State state;
		public Long moneySupply;
		public Integer decimalPoint;
	}

	private final Storage storage;

	public ColoredCoinService(Storage storage) {
		this.storage = storage;
	}

	/**
	 * Get a colored coin information.
	 *
	 * @param id
	 * @return
	 * @throws RemotePeerException
	 * @throws IOException
	 */
	public Info getInfo(String id, int timestamp) throws RemotePeerException, IOException {
		IAccount account = getColoredAccount(id);

		Info info = new Info();
		info.state = State.Unauthorized;

		if (account == null) {
			return info;
		}

		ColoredCoin coloredCoin = AccountProperties.getColoredCoinRegistrationData(account);
		if (coloredCoin == null) {
			return info;
		}

		if (coloredCoin.getTimestamp() > timestamp) {
			return info;
		}

		info.state = State.OK;
		info.decimalPoint = coloredCoin.getDecimalPoint();
		info.moneySupply = coloredCoin.getMoneySupply();

		return info;

	}

	IAccount getColoredAccount(String id) throws RemotePeerException {
		long accountID;
		try {
			accountID = ColoredCoinId.convert(id);
		} catch (IllegalArgumentException e) {
			throw new RemotePeerException(e);
		}

		final ILedger ledgerState = Ledgers.newReadOnlyLedger(storage.getConnection(),
				storage.getLastBlock().getSnapshot());
		return ledgerState.getAccount(accountID);
	}

}
