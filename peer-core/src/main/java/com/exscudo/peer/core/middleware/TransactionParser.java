package com.exscudo.peer.core.middleware;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;

public class TransactionParser {
    private static ITransactionParser instance;

    public static void init(ITransactionParser transactionParser) {
        instance = transactionParser;
    }

    public static ILedgerAction[] parse(Transaction transaction) throws ValidateException {
        return instance.parse(transaction);
    }

    public static AccountID getRecipient(Transaction transaction) throws ValidateException {
        return instance.getRecipient(transaction);
    }
}
