package com.exscudo.peer.core.backlog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.blockchain.IBlockchainProvider;
import com.exscudo.peer.core.blockchain.TransactionProvider;
import com.exscudo.peer.core.common.TimeProvider;
import com.exscudo.peer.core.common.exceptions.LifecycleException;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.data.transaction.ITransactionHandler;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.ledger.AbstractLedger;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;

/**
 * Basic implementation of {@code IBacklogService}.
 * <p>
 * Pre-validation of transactions before putting in Backlog.
 *
 * @see IBacklog
 */
public class Backlog implements IBacklog {

    private final BacklogStorage backlogStorage = new BacklogStorage();

    private final IBlockchainProvider blockchain;
    private final LedgerProvider ledgerProvider;
    private final TransactionProvider transactionProvider;
    private final IFork fork;
    private final TimeProvider timeProvider;
    private NotImmutableCachedLedger ledger;
    private TransactionContext ctx;
    private ITransactionHandler handler;

    public Backlog(IFork fork,
                   IBlockchainProvider blockchain,
                   LedgerProvider ledgerProvider,
                   TransactionProvider transactionProvider,
                   TimeProvider timeProvider) {
        this.fork = fork;
        this.blockchain = blockchain;
        this.ledgerProvider = ledgerProvider;
        this.transactionProvider = transactionProvider;
        this.timeProvider = timeProvider;

        InitContext();
    }

    @Override
    public synchronized boolean put(Transaction transaction) throws ValidateException {

        if (transaction.isFuture(timeProvider.get() + Constant.MAX_LATENCY)) {
            throw new LifecycleException();
        }

        TransactionID txID = transaction.getID();
        if (backlogStorage.contains(txID)) {
            return false;
        }

        if (transactionProvider.containsTransaction(txID)) {
            return true;
        }

        // ValidateException can throws after ledger changes.
        NotImmutableCachedLedger newLedger = new NotImmutableCachedLedger(ledger);
        handler.run(transaction, newLedger, ctx);
        ledger.putAll(newLedger);

        backlogStorage.put(transaction);

        return true;
    }

    @Override
    public Transaction get(TransactionID id) {
        return backlogStorage.get(id);
    }

    @Override
    public synchronized List<Transaction> copyAndClear() {
        InitContext();
        return backlogStorage.copyAndClear();
    }

    @Override
    public int size() {
        return backlogStorage.size();
    }

    @Override
    public Iterator<TransactionID> iterator() {
        return backlogStorage.iterator();
    }

    private void InitContext() {
        Block block = blockchain.getLastBlock();
        ledger = new NotImmutableCachedLedger(ledgerProvider.getLedger(block));
        int timestamp = block.getTimestamp() + Constant.BLOCK_PERIOD;

        handler = fork.getTransactionExecutor(timestamp);
        ctx = new TransactionContext(timestamp);
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
            throw new RuntimeException("NotImplemented");
        }

        @Override
        public void save() {
            throw new RuntimeException("NotImplemented");
        }

        public void putAll(NotImmutableCachedLedger newLedger) {
            cache.putAll(newLedger.cache);
        }

        @Override
        public Iterator<Account> iterator() {
            throw new RuntimeException("NotImplemented");
        }
    }
}
