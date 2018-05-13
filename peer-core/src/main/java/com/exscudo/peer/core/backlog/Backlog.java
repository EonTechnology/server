package com.exscudo.peer.core.backlog;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.backlog.events.BacklogEventManager;
import com.exscudo.peer.core.backlog.events.RejectionReason;
import com.exscudo.peer.core.blockchain.IBlockchainProvider;
import com.exscudo.peer.core.blockchain.storage.DbBlock;
import com.exscudo.peer.core.blockchain.storage.DbNestedTransaction;
import com.exscudo.peer.core.blockchain.storage.DbTransaction;
import com.exscudo.peer.core.common.ITimeProvider;
import com.exscudo.peer.core.common.ImmutableTimeProvider;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.ledger.AbstractLedger;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.LedgerActionContext;
import com.exscudo.peer.core.middleware.TransactionParser;
import com.exscudo.peer.core.middleware.TransactionValidator;
import com.exscudo.peer.core.middleware.ValidationResult;
import com.exscudo.peer.core.storage.Storage;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.ThreadLocalSelectArg;
import com.j256.ormlite.stmt.Where;

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
    private final IFork fork;
    private final TimeProvider timeProvider;

    private NotImmutableCachedLedger ledger;
    private LedgerActionContext ctx;
    private TransactionValidator transactionValidator;

    // region Prepared Statements

    private QueryBuilder<DbTransaction, Long> existBuilder;
    private ArgumentHolder vID = new ThreadLocalSelectArg();

    private QueryBuilder<DbNestedTransaction, Long> nestedTxByBlockQueryBuilder;
    private ArgumentHolder vIndexes = new ThreadLocalSelectArg();

    private QueryBuilder<DbBlock, Long> blockQueryBuilder;

    // endregion

    public Backlog(BacklogEventManager backlogEventManager,
                   IFork fork,
                   Storage storage,
                   IBlockchainProvider blockchain,
                   LedgerProvider ledgerProvider,
                   TimeProvider timeProvider) {

        this.backlogEventManager = backlogEventManager;
        this.fork = fork;
        this.storage = storage;
        this.blockchain = blockchain;
        this.ledgerProvider = ledgerProvider;
        this.timeProvider = timeProvider;

        InitContext(blockchain.getLastBlock());
    }

    @Override
    public synchronized void put(Transaction transaction) throws ValidateException {

        backlogEventManager.raiseUpdating(this, transaction);

        if (backlogContains(transaction)) {
            backlogEventManager.raiseRejected(this, transaction, RejectionReason.ALREADY_IN_BACKLOG);
            return;
        }

        if (blockchainContains(transaction)) {
            backlogEventManager.raiseRejected(this, transaction, RejectionReason.ALREADY_CONFIRMED);
            return;
        }

        int difficulty = fork.getDifficulty(transaction, ctx.getTimestamp());
        transaction.setLength(difficulty);
        ValidationResult r = transactionValidator.validate(transaction, ledger);
        if (r.hasError) {
            backlogEventManager.raiseRejected(this, transaction, RejectionReason.INVALID);
            throw r.cause;
        }

        // ValidateException can throws after ledger changes.
        NotImmutableCachedLedger newLedger = new NotImmutableCachedLedger(ledger);

        try {
            ILedgerAction[] actions = TransactionParser.parse(transaction);
            for (ILedgerAction action : actions) {
                action.run(newLedger, ctx);
            }
        } catch (ValidateException e) {
            backlogEventManager.raiseRejected(this, transaction, RejectionReason.INVALID);
            throw e;
        }

        ledger.putAll(newLedger);

        backlogStorage.put(transaction);
        backlogEventManager.raiseUpdated(this, transaction);
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

    private boolean backlogContains(Transaction transaction) {

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
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean blockchainContains(Transaction transaction) {

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

                LinkedList<Long> indexes = new LinkedList<>();
                for (String key : transaction.getNestedTransactions().keySet()) {
                    indexes.add((new TransactionID(key)).getValue());
                }

                if (nestedTxByBlockQueryBuilder == null) {
                    Dao<DbNestedTransaction, Long> nestedTxDao =
                            DaoManager.createDao(storage.getConnectionSource(), DbNestedTransaction.class);
                    nestedTxByBlockQueryBuilder = nestedTxDao.queryBuilder();
                    nestedTxByBlockQueryBuilder.selectColumns("block_id").distinct().where().in("id", vIndexes);
                }
                vIndexes.setValue(indexes);

                if (blockQueryBuilder == null) {
                    Dao<DbBlock, Long> blockDao = DaoManager.createDao(storage.getConnectionSource(), DbBlock.class);
                    blockQueryBuilder = blockDao.queryBuilder();
                    Where<DbBlock, Long> w = blockQueryBuilder.where();
                    w.eq("tag", 1);
                    w.and().in("id", nestedTxByBlockQueryBuilder);
                }

                countOf = blockQueryBuilder.countOf();
                if (countOf != 0) {
                    return true;
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
        ctx = new LedgerActionContext(timestamp, fork);
        transactionValidator = TransactionValidator.getAllValidators(fork, blockTimeProvider, peerTimeProvider);
    }

    private static class NotImmutableCachedLedger extends AbstractLedger {

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
}
