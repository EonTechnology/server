package com.exscudo.eon.app.api.bot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.exscudo.peer.core.backlog.IBacklog;
import com.exscudo.peer.core.blockchain.IBlockchainProvider;
import com.exscudo.peer.core.common.Format;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.eon.ledger.AccountProperties;
import com.exscudo.peer.eon.ledger.state.BalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ColoredCoinProperty;
import com.exscudo.peer.eon.ledger.state.GeneratingBalanceProperty;
import com.exscudo.peer.eon.ledger.state.ValidationModeProperty;
import com.exscudo.peer.eon.ledger.state.VotePollsProperty;
import com.exscudo.peer.tx.ColoredCoinID;
import com.exscudo.peer.tx.TransactionType;

/**
 * Account status service.
 */
public class AccountBotService {

    private final IBacklog backlog;
    private final IBlockchainProvider blockchain;
    private final LedgerProvider ledgerProvider;

    public AccountBotService(IBacklog backlog, LedgerProvider ledgerProvider, IBlockchainProvider blockchain) {
        this.backlog = backlog;
        this.blockchain = blockchain;
        this.ledgerProvider = ledgerProvider;
    }

    /**
     * Get account status
     *
     * @param id account ID
     * @return
     * @throws RemotePeerException
     * @throws IOException
     */
    public State getState(String id) throws RemotePeerException, IOException {

        Account account = getAccount(id);
        if (account != null) {
            return State.OK;
        }

        IBacklog backlog = this.backlog;
        for (TransactionID item : backlog) {
            Transaction transaction = backlog.get(item);

            if (transaction != null && transaction.getType() == TransactionType.Registration) {

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
     * @param id account ID
     * @return
     * @throws RemotePeerException
     * @throws IOException
     */
    public Info getInformation(String id) throws RemotePeerException, IOException {

        Account account = getAccount(id);

        Info info = new Info();
        if (account == null) {
            info.state = State.Unauthorized;
        } else {

            info.state = State.OK;
            info.publicKey = Format.convert(AccountProperties.getRegistration(account).getPublicKey());

            ValidationModeProperty validationMode = AccountProperties.getValidationMode(account);
            if (validationMode.getTimestamp() == -1) {
                info.signType = SignType.Normal;
            } else {

                Quorum quorum = null;
                VotingRights rights = null;

                // rights
                HashMap<String, Integer> delegates = new HashMap<>();
                for (Map.Entry<AccountID, Integer> e : validationMode.delegatesEntrySet()) {
                    delegates.put(e.getKey().toString(), e.getValue());
                }
                if (!delegates.isEmpty()) {
                    rights = new VotingRights();
                    rights.delegates = delegates;
                }

                // quorums
                HashMap<Integer, Integer> types = new HashMap<>();
                for (Map.Entry<Integer, Integer> e : validationMode.quorumsEntrySet()) {
                    types.put(e.getKey(), e.getValue());
                }
                if (validationMode.getBaseQuorum() != ValidationModeProperty.MAX_QUORUM || !types.isEmpty()) {
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

            VotePollsProperty voter = AccountProperties.getVoter(account);
            if (voter.getTimestamp() != -1) {
                HashMap<String, Integer> vMap = new HashMap<>();
                for (Map.Entry<AccountID, Integer> entry : voter.pollsEntrySet()) {
                    vMap.put(entry.getKey().toString(), entry.getValue());
                }
                info.voter = vMap;
            }

            ColoredCoinProperty coloredCoin = AccountProperties.getColoredCoin(account);
            if (coloredCoin.isIssued()) {
                info.coloredCoin = new ColoredCoinID(account.getID()).toString();
            }

            // TODO: add current generating balance
            GeneratingBalanceProperty generatingBalance = AccountProperties.getDeposit(account);
            info.deposit = generatingBalance.getValue();

            BalanceProperty balance = AccountProperties.getBalance(account);
            info.amount = balance.getValue();
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
        Account account = getAccount(id);

        EONBalance balance = new EONBalance();
        if (account == null) {
            balance.state = State.Unauthorized;
        } else {
            balance.state = State.OK;

            BalanceProperty b = AccountProperties.getBalance(account);
            balance.amount = b.getValue();

            ColoredBalanceProperty coloredBalance = AccountProperties.getColoredBalance(account);
            if (coloredBalance != null) {
                Map<String, Long> cMap = new HashMap<>();
                for (ColoredCoinID coinID : coloredBalance) {
                    cMap.put(coinID.toString(), coloredBalance.getBalance(coinID));
                }
                if (!cMap.isEmpty()) {
                    balance.coloredCoins = cMap;
                }
            }
        }
        return balance;
    }

    Account getAccount(String id) throws RemotePeerException {

        AccountID accountID;
        try {
            accountID = new AccountID(id);
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        }

        final ILedger ledgerState = ledgerProvider.getLedger(blockchain.getLastBlock());

        return ledgerState.getAccount(accountID);
    }

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
}
