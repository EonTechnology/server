package com.exscudo.peer.core.backlog;

import java.util.Iterator;
import java.util.List;

import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.blockchain.IBlockchainService;
import com.exscudo.peer.core.blockchain.TransactionProvider;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.data.transaction.ITransactionHandler;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.importer.IFork;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.ledger.LedgerProvider;

/**
 * Basic implementation of {@code IBacklogService}.
 * <p>
 * Pre-validation of transactions before putting in Backlog.
 *
 * @see IBacklogService
 */
public class Backlog implements IBacklogService {

    private final BacklogStorage backlogStorage = new BacklogStorage();

    private final IBlockchainService blockchain;
    private final LedgerProvider ledgerProvider;
    private final TransactionProvider transactionProvider;
    private final IFork fork;
    private Block block;
    private ILedger ledger;
    private TransactionContext ctx;

    public Backlog(IFork fork,
                   IBlockchainService blockchain,
                   LedgerProvider ledgerProvider,
                   TransactionProvider transactionProvider) {
        this.fork = fork;
        this.blockchain = blockchain;
        this.ledgerProvider = ledgerProvider;
        this.transactionProvider = transactionProvider;

        InitContext();
    }

    @Override
    public synchronized boolean put(Transaction transaction) throws ValidateException {

        TransactionID txID = transaction.getID();
        if (backlogStorage.contains(txID)) {
            return false;
        }

        if (transactionProvider.containsTransaction(txID)) {
            return true;
        }

        int timestamp = block.getTimestamp() + Constant.BLOCK_PERIOD;
        ITransactionHandler handler = fork.getTransactionExecutor(timestamp);

        ledger = handler.run(transaction, ledger, ctx);

        backlogStorage.put(transaction);

        return true;
    }

    @Override
    public Transaction get(TransactionID id) {
        return backlogStorage.get(id);
    }

    @Override
    public boolean contains(TransactionID id) {
        return backlogStorage.contains(id);
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
        block = blockchain.getLastBlock();
        ledger = ledgerProvider.getLedger(block);
        int timestamp = block.getTimestamp() + Constant.BLOCK_PERIOD;
        ctx = new TransactionContext(timestamp);
    }
}
