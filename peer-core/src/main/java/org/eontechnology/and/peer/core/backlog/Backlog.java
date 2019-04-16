package org.eontechnology.and.peer.core.backlog;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import com.j256.ormlite.stmt.Where;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.IFork;
import org.eontechnology.and.peer.core.backlog.events.BacklogEventManager;
import org.eontechnology.and.peer.core.backlog.events.RejectionReason;
import org.eontechnology.and.peer.core.blockchain.IBlockchainProvider;
import org.eontechnology.and.peer.core.blockchain.storage.DbBlock;
import org.eontechnology.and.peer.core.blockchain.storage.DbNestedTransaction;
import org.eontechnology.and.peer.core.blockchain.storage.DbTransaction;
import org.eontechnology.and.peer.core.common.ITimeProvider;
import org.eontechnology.and.peer.core.common.ITransactionEstimator;
import org.eontechnology.and.peer.core.common.ImmutableTimeProvider;
import org.eontechnology.and.peer.core.common.exceptions.DataAccessException;
import org.eontechnology.and.peer.core.common.exceptions.ValidateException;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.Block;
import org.eontechnology.and.peer.core.data.Transaction;
import org.eontechnology.and.peer.core.data.identifier.AccountID;
import org.eontechnology.and.peer.core.data.identifier.TransactionID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.ledger.LedgerProvider;
import org.eontechnology.and.peer.core.middleware.ILedgerAction;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.core.middleware.LedgerActionContext;
import org.eontechnology.and.peer.core.middleware.TransactionValidator;
import org.eontechnology.and.peer.core.middleware.TransactionValidatorFabric;
import org.eontechnology.and.peer.core.middleware.ValidationResult;
import org.eontechnology.and.peer.core.storage.Storage;

/**
 * Basic implementation of {@code IBacklog}.
 * <p>
 * Pre-validation of transactions before putting in Backlog.
 *
 * @see IBacklog
 */
public class Backlog implements IBacklog {

    private final BacklogStorage backlogStorage = new BacklogStorage();
    private final BacklogEventManager backlogEventManager;
    private final Storage storage;

    private final IBlockchainProvider blockchain;
    private final LedgerProvider ledgerProvider;
    private final ITimeProvider timeProvider;
    private final IFork fork;
    private final TransactionValidatorFabric transactionValidatorFabric;
    private final ITransactionEstimator estimator;
    private final String nestedExistMSG = "Invalid sequence. Transaction already exist.";

    private NotImmutableCachedLedger ledger;
    private LedgerActionContext ctx;
    private TransactionValidator transactionValidator;

    // region Prepared Statements

    private QueryBuilder<DbTransaction, Long> existBuilder;
    private ArgumentHolder vID = new ThreadLocalSelectArg();

    private QueryBuilder<DbNestedTransaction, Long> nestedTxByBlockQueryBuilder;
    private ThreadLocalIterator vIndexes = new ThreadLocalIterator();

    private QueryBuilder<DbBlock, Long> blockQueryBuilder;

    // endregion

    public Backlog(IFork fork,
                   BacklogEventManager backlogEventManager,
                   Storage storage,
                   IBlockchainProvider blockchain,
                   LedgerProvider ledgerProvider,
                   ITimeProvider timeProvider,
                   TransactionValidatorFabric transactionValidatorFabric,
                   ITransactionEstimator estimator) {

        this.fork = fork;
        this.backlogEventManager = backlogEventManager;
        this.storage = storage;
        this.blockchain = blockchain;
        this.ledgerProvider = ledgerProvider;
        this.timeProvider = timeProvider;
        this.transactionValidatorFabric = transactionValidatorFabric;
        this.estimator = estimator;

        InitContext(blockchain.getLastBlock());
    }

    @Override
    public synchronized void put(Transaction transaction) throws ValidateException {

        try {
            backlogEventManager.raiseUpdating(this, transaction);

            if (backlogContains(transaction)) {
                backlogEventManager.raiseRejected(this, transaction, RejectionReason.ALREADY_IN_BACKLOG);
                return;
            }

            if (blockchainContains(transaction)) {
                backlogEventManager.raiseRejected(this, transaction, RejectionReason.ALREADY_CONFIRMED);
                return;
            }

            int difficulty = estimator.estimate(transaction);
            transaction.setLength(difficulty);
            ValidationResult r = transactionValidator.validate(transaction, ledger);
            if (r.hasError) {
                throw r.cause;
            }

            ITransactionParser parser = fork.getParser(ctx.getTimestamp());
            // ValidateException can throws after ledger changes.
            NotImmutableCachedLedger newLedger = new NotImmutableCachedLedger(ledger);

            ILedgerAction[] actions = parser.parse(transaction);
            for (ILedgerAction action : actions) {
                action.run(newLedger, ctx);
            }

            ledger.putAll(newLedger);

            backlogStorage.put(transaction);
            backlogEventManager.raiseUpdated(this, transaction);
        } catch (ValidateException ex) {
            backlogEventManager.raiseRejected(this, transaction, RejectionReason.INVALID);
            throw ex;
        }
    }

    @Override
    public Transaction get(TransactionID id) {
        return backlogStorage.get(id);
    }

    synchronized List<Transaction> copyAndClear() {
        InitContext(blockchain.getLastBlock());
        return backlogStorage.copyAndClear();
    }

    public int size() {
        return backlogStorage.size();
    }

    private boolean backlogContains(Transaction transaction) throws ValidateException {

        TransactionID txID = transaction.getID();

        if (backlogStorage.contains(txID)) {
            return true;
        }

        if (transaction.hasNestedTransactions()) {

            Set<String> aSet = new HashSet<>(transaction.getNestedTransactions().keySet());

            Iterator<TransactionID> keys = backlogStorage.iterator();
            while (keys.hasNext()) {

                Transaction v = backlogStorage.get(keys.next());
                if (v != null && v.hasNestedTransactions()) {

                    Set<String> bSet = new HashSet<>(v.getNestedTransactions().keySet());

                    bSet.retainAll(aSet);
                    if (!bSet.isEmpty()) {
                        throw new ValidateException(nestedExistMSG);
                    }
                }
            }
        }

        return false;
    }

    private boolean blockchainContains(Transaction transaction) throws ValidateException {

        try {

            if (existBuilder == null) {
                Dao<DbTransaction, Long> dao = DaoManager.createDao(storage.getConnectionSource(), DbTransaction.class);
                existBuilder = dao.queryBuilder();
                existBuilder.where().eq("id", vID).and().eq("tag", 1);
            }

            vID.setValue(transaction.getID().getValue());
            long countOf = existBuilder.countOf();
            if (countOf != 0) {
                return true;
            }

            // check nested transaction
            if (transaction.hasNestedTransactions()) {

                List<Object> list = vIndexes.get();
                list.clear();
                for (String key : transaction.getNestedTransactions().keySet()) {
                    list.add(new TransactionID(key).getValue());
                }

                if (nestedTxByBlockQueryBuilder == null) {
                    Dao<DbNestedTransaction, Long> nestedTxDao =
                            DaoManager.createDao(storage.getConnectionSource(), DbNestedTransaction.class);
                    nestedTxByBlockQueryBuilder = nestedTxDao.queryBuilder();
                    nestedTxByBlockQueryBuilder.selectColumns("block_id").distinct().where().in("id", vIndexes);
                }

                if (blockQueryBuilder == null) {
                    Dao<DbBlock, Long> blockDao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);
                    blockQueryBuilder = blockDao.queryBuilder();
                    Where<DbBlock, Long> w = blockQueryBuilder.where();
                    w.eq("tag", 1);
                    w.and().in("id", nestedTxByBlockQueryBuilder);
                }

                countOf = blockQueryBuilder.countOf();
                list.clear();
                if (countOf != 0) {
                    throw new ValidateException(nestedExistMSG);
                }
            }

            return false;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public Iterator<TransactionID> iterator() {
        return backlogStorage.iterator();
    }

    private void InitContext(Block block) {
        int timestamp = block.getTimestamp() + Constant.BLOCK_PERIOD;

        ITimeProvider blockTimeProvider = new ImmutableTimeProvider(timestamp);
        ITimeProvider peerTimeProvider = new ITimeProvider() {
            @Override
            public int get() {
                return timeProvider.get() + Constant.MAX_LATENCY;
            }
        };

        ledger = new NotImmutableCachedLedger(ledgerProvider.getLedger(block));
        ctx = new LedgerActionContext(timestamp);
        transactionValidator = transactionValidatorFabric.getAllValidators(blockTimeProvider, peerTimeProvider);
    }

    private static class NotImmutableCachedLedger implements ILedger {

        Map<AccountID, Account> cache = Collections.synchronizedMap(new HashMap<>());
        private ILedger base;

        public NotImmutableCachedLedger(ILedger base) {

            this.base = base;
        }

        @Override
        public Account getAccount(AccountID accountID) {
            Account account = cache.get(accountID);
            if (account == null) {
                account = base.getAccount(accountID);
            }
            return account;
        }

        @Override
        public ILedger putAccount(Account account) {
            cache.put(account.getID(), account);
            return this;
        }

        @Override
        public String getHash() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save() {
            throw new UnsupportedOperationException();
        }

        public void putAll(NotImmutableCachedLedger newLedger) {
            cache.putAll(newLedger.cache);
        }

        @Override
        public Iterator<Account> iterator() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ThreadLocalIterator extends ThreadLocal<List<Object>> implements Iterable {
        @Override
        protected List<Object> initialValue() {
            return new LinkedList<>();
        }

        @Override
        public Iterator iterator() {
            return get().iterator();
        }
    }
}
