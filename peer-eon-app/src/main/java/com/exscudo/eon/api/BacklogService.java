package com.exscudo.eon.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import com.exscudo.peer.core.backlog.IBacklog;
import com.exscudo.peer.core.common.Loggers;
import com.exscudo.peer.core.common.exceptions.RemotePeerException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.TransactionID;

public class BacklogService {
    private static final int TRANSACTION_LAST_PAGE_SIZE = 10;
    private final IBacklog backlog;

    public BacklogService(IBacklog backlog) {
        this.backlog = backlog;
    }

    public Transaction getById(String id) throws RemotePeerException, IOException {

        try {

            TransactionID transactionID = new TransactionID(id);
            return backlog.get(transactionID);
        } catch (IllegalArgumentException e) {

            throw new RemotePeerException(e);
        } catch (Exception e) {

            Loggers.error(BacklogService.class, e);
            throw new RemotePeerException();
        }
    }

    public List<Transaction> getByAccountId(String id) throws RemotePeerException, IOException {
        try {

            AccountID accountID = new AccountID(id);

            List<Transaction> list = new ArrayList<>();
            for (TransactionID item : backlog) {

                Transaction transaction = backlog.get(item);
                if (transaction != null) {
                    if (transaction.getSenderID().equals(accountID)) {
                        list.add(transaction);
                        // TODO : review
                    } else if (transaction.getData() != null &&
                            transaction.getData().containsKey("recipient") &&
                            id.equals(transaction.getData().get("recipient").toString())) {
                        list.add(transaction);
                    }
                }
            }

            return list;
        } catch (IllegalArgumentException e) {
            throw new RemotePeerException(e);
        } catch (Exception e) {
            Loggers.error(BacklogService.class, e);
            throw new RemotePeerException();
        }
    }

    public Collection<Transaction> getLastPage() throws RemotePeerException, IOException {
        try {

            TreeSet<Transaction> set = new TreeSet<>(new TransactionComparator());

            for (TransactionID id : backlog) {
                Transaction tr = backlog.get(id);
                if (tr == null) {
                    continue;
                }
                set.add(tr);
                if (set.size() > TRANSACTION_LAST_PAGE_SIZE) {
                    set.pollLast();
                }
            }

            return set;
        } catch (Exception e) {
            Loggers.error(BacklogService.class, e);
            throw new RemotePeerException();
        }
    }

    private static class TransactionComparator implements Comparator<Transaction> {

        @Override
        public int compare(Transaction o1, Transaction o2) {
            return Long.compare(o2.getTimestamp(), o1.getTimestamp());
        }
    }
}
