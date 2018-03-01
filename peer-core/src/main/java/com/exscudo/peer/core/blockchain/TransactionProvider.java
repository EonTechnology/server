package com.exscudo.peer.core.blockchain;

import java.sql.SQLException;
import java.util.List;

import com.exscudo.peer.core.common.exceptions.DataAccessException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.storage.Storage;
import com.exscudo.peer.core.storage.utils.TransactionHelper;

public class TransactionProvider {
    private final TransactionHelper transactionHelper;

    public TransactionProvider(Storage storage) {
        transactionHelper = storage.getTransactionHelper();
    }

    public boolean containsTransaction(TransactionID id) {
        try {
            return transactionHelper.contains(id);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public Transaction getTransaction(TransactionID id) {
        try {
            Transaction tx = transactionHelper.get(id);
            return tx;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public List<Transaction> getForkedTransactions(int timestamp) {
        try {
            return transactionHelper.getForked(timestamp);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }
}
