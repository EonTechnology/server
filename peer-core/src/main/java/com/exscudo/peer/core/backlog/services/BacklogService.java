package com.exscudo.peer.core.backlog.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import com.exscudo.peer.core.backlog.Backlog;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.middleware.TransactionParser;

public class BacklogService {
    private final Backlog backlog;

    public BacklogService(Backlog backlog) {
        this.backlog = backlog;
    }

    /**
     * Returns the transaction by specified id.
     *
     * @param id for search
     * @return transaction or null
     * @throws NullPointerException if the specified id is null
     */
    public Transaction get(TransactionID id) {
        return backlog.get(Objects.requireNonNull(id));
    }

    /**
     * Adds passed <code>transaction</code> to a Backlog.
     *
     * @param transaction transaction to add to the buffer. can not be null.
     * @throws ValidateException    If some property of the specified <code>transaction</code>
     *                              prevents it from being processed.
     * @throws NullPointerException if the specified transaction is null
     */
    public void put(Transaction transaction) throws ValidateException {
        backlog.put(Objects.requireNonNull(transaction));
    }

    /**
     * Returns the transactions for the specified account.
     * <p>
     *
     * @param id by the account
     * @return list of the transactions
     * @throws NullPointerException if the specified id is null
     */
    public List<Transaction> getForAccount(AccountID id) {
        Objects.requireNonNull(id);

        List<Transaction> list = new ArrayList<>();
        for (TransactionID item : backlog) {

            Transaction transaction = backlog.get(item);
            if (transaction == null) {
                continue;
            }

            if (id.equals(transaction.getSenderID())) {
                list.add(transaction);
                continue;
            }

            try {
                AccountID recipient = TransactionParser.getRecipient(transaction);
                if (id.equals(recipient)) {
                    list.add(transaction);
                }
            } catch (ValidateException ignored) {
            }
        }

        return list;
    }

    /**
     * Returns the last transactions from Backlog.
     * <p>
     * Returns sorted by timestamp
     *
     * @param count of transactions
     * @return set of the transactions
     */
    public Collection<Transaction> getLatest(int count) {

        TreeSet<Transaction> set = new TreeSet<>(new TransactionComparator());

        for (TransactionID id : backlog) {
            Transaction transaction = backlog.get(id);
            if (transaction == null) {
                continue;
            }

            set.add(transaction);
            if (set.size() > count) {
                set.pollLast();
            }
        }

        return set;
    }

    private static class TransactionComparator implements Comparator<Transaction> {

        @Override
        public int compare(Transaction o1, Transaction o2) {
            return Long.compare(o2.getTimestamp(), o1.getTimestamp());
        }
    }
}
