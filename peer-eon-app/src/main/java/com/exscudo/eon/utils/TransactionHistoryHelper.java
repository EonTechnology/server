package com.exscudo.eon.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.exscudo.peer.core.backlog.IBacklogService;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.storage.utils.AccountHelper;

public class TransactionHistoryHelper {
    private final static int PAGE_SIZE = 20;

    public static AccountID getAccountId(String accountId) {
        return new AccountID(accountId);
    }

    public static List<Transaction> getCommittedPage(AccountHelper accountHelper,
                                                     String accountId,
                                                     int page) throws SQLException {
        AccountID accID = getAccountId(accountId);
        return accountHelper.getTransactions(accID, page * PAGE_SIZE, PAGE_SIZE);
    }

    public static List<Transaction> getUncommitted(IBacklogService backlog, String accountId) {
        List<Transaction> items = new ArrayList<>();

        AccountID accID = getAccountId(accountId);

        for (TransactionID item : backlog) {
            Transaction transaction = backlog.get(item);
            if (transaction != null) {
                if (transaction.getSenderID().equals(accID)) {
                    items.add(transaction);
                } else if (transaction.getData() != null &&
                        transaction.getData().containsKey("recipient") &&
                        accountId.equals(transaction.getData().get("recipient").toString())) {
                    items.add(transaction);
                }
            }
        }

        return items;
    }
}
