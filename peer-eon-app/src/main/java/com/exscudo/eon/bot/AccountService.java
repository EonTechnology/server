package com.exscudo.eon.bot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.exceptions.RemotePeerException;
import com.exscudo.peer.core.services.IAccount;
import com.exscudo.peer.core.services.IBacklogService;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.utils.Format;
import com.exscudo.peer.eon.TransactionType;
import com.exscudo.peer.eon.state.Balance;
import com.exscudo.peer.eon.state.ColoredBalance;
import com.exscudo.peer.eon.state.ColoredCoin;
import com.exscudo.peer.eon.state.GeneratingBalance;
import com.exscudo.peer.eon.state.ValidationMode;
import com.exscudo.peer.eon.state.Voter;
import com.exscudo.peer.eon.transactions.utils.AccountProperties;
import com.exscudo.peer.eon.utils.ColoredCoinId;
import com.exscudo.peer.store.sqlite.Storage;
import com.exscudo.peer.store.sqlite.merkle.Ledgers;

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

		/**
		 * Account is unauthorized
		 */
		public static final State Unauthorized = new State(401, "Unauthorized");

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
		public String publicKey;
		public long amount;
		public long deposit;

		public String signType;
		public VotingRights votingRights;
		public Quorum quorum;
		public String seed;
		public Map<String, Integer> voter;
		public String coloredCoin;
	}

	/**
	 * Type of transaction confirmation
	 * <p>
	 * The type determines the features of the account (e.g., Inbound transaction
	 * processing rules)
	 */
	public static class SignType {

		public static final String Normal = "normal";

		public static final String Public = "public";

		public static final String MFA = "mfa";

	}

	/**
	 * Determines the distribution of votes
	 */
	public static class VotingRights {
		public Integer weight;
		public Map<String, Integer> delegates;
	}

	/**
	 * Transaction confirmation settings
	 */
	public static class Quorum {
		public Integer quorum;
		public Map<Integer, Integer> quorumByTypes;
	}

	/**
	 * Account deposit
	 */
	public static class Deposit {
		public Long value;
	}

	/**
	 * Account balance
	 */
	public static class EONBalance {
		public State state;
		public long amount;
		public Map<String, Long> coloredCoins;
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

		IAccount account = getAccount(id);
		if (account != null) {
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

		IAccount account = getAccount(id);

		Info info = new Info();
		if (account == null) {
			info.state = State.Unauthorized;
		} else {

			info.state = State.OK;
			info.publicKey = Format.convert(AccountProperties.getPublicKey(account));

			ValidationMode validationMode = AccountProperties.getValidationMode(account);
			if (validationMode == null) {
				info.signType = SignType.Normal;
			} else {

				Quorum quorum = null;
				VotingRights rights = null;

				// rights
				HashMap<String, Integer> delegates = new HashMap<String, Integer>();
				for (Map.Entry<Long, Integer> e : validationMode.delegatesEntrySet()) {
					delegates.put(Format.ID.accountId(e.getKey()), e.getValue());
				}
				if (!delegates.isEmpty()) {
					rights = new VotingRights();
					rights.delegates = delegates;
				}

				// quorums
				HashMap<Integer, Integer> types = new HashMap<Integer, Integer>();
				for (Map.Entry<Integer, Integer> e : validationMode.quorumsEntrySet()) {
					types.put(e.getKey(), e.getValue());
				}
				if (validationMode.getBaseQuorum() != ValidationMode.MAX_QUORUM || !types.isEmpty()) {
					quorum = new Quorum();
					quorum.quorum = validationMode.getBaseQuorum();
					if (!types.isEmpty()) {
						quorum.quorumByTypes = types;
					}
				}

				if (validationMode.isNormal()) {
					info.signType = SignType.Normal;
				} else if (validationMode.isPublic()) {
					if (rights.delegates == null) {
						throw new IllegalStateException(id);
					}
					info.signType = SignType.Public;
					info.seed = validationMode.getSeed();
				} else if (validationMode.isMultiFactor()) {
					rights.weight = validationMode.getBaseWeight();
					info.signType = SignType.MFA;
				}

				info.votingRights = rights;
				info.quorum = quorum;
			}

			Voter voter = AccountProperties.getVoter(account);
			if (voter != null) {
				HashMap<String, Integer> vMap = new HashMap<String, Integer>();
				for (Map.Entry<Long, Integer> entry : voter.pollsEntrySet()) {
					vMap.put(Format.ID.accountId(entry.getKey()), entry.getValue());
				}
				info.voter = vMap;
			}

			ColoredCoin coloredCoin = AccountProperties.getColoredCoinRegistrationData(account);
			if (coloredCoin != null) {
				info.coloredCoin = ColoredCoinId.convert(account.getID());
			}

			// TODO: add current generating balance
			GeneratingBalance generatingBalance = AccountProperties.getDeposit(account);
			if (generatingBalance == null) {
				info.deposit = 0;
			} else {
				info.deposit = generatingBalance.getValue();
			}

			Balance balance = AccountProperties.getBalance(account);
			if (balance == null) {
				info.amount = 0;
			} else {
				info.amount = balance.getValue();
			}

		}

		return info;
	}

	/**
	 * Gets account balance.
	 *
	 * @param id
	 * @return
	 * @throws RemotePeerException
	 * @throws IOException
	 */
	public EONBalance getBalance(String id) throws RemotePeerException, IOException {
		IAccount account = getAccount(id);

		EONBalance balance = new EONBalance();
		if (account == null) {
			balance.state = State.Unauthorized;
		} else {
			balance.state = State.OK;

			Balance b = AccountProperties.getBalance(account);
			if (b == null) {
				balance.amount = 0;
			} else {
				balance.amount = b.getValue();
			}

			ColoredBalance coloredBalance = AccountProperties.getColoredBalance(account);
			if(coloredBalance != null) {
				Map<String, Long> cMap = new HashMap<>();
				for(Map.Entry<Long, Long> e : coloredBalance.balancesEntrySet()) {
					cMap.put(ColoredCoinId.convert(e.getKey()), e.getValue());
				}
				if(!cMap.isEmpty()){
					balance.coloredCoins = cMap;
				}
			}
		}
		return balance;

	}

	IAccount getAccount(String id) throws RemotePeerException {

		long accountID;
		try {
			accountID = Format.ID.accountId(id);
		} catch (IllegalArgumentException e) {
			throw new RemotePeerException(e);
		}

		final ILedger ledgerState = Ledgers.newReadOnlyLedger(storage.getConnection(),
				storage.getLastBlock().getSnapshot());

		return ledgerState.getAccount(accountID);

	}

}
