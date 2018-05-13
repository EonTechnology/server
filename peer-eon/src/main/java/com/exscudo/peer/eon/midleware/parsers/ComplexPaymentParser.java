package com.exscudo.peer.eon.midleware.parsers;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.actions.FeePaymentAction;

public class ComplexPaymentParser implements ITransactionParser {
    private final ITransactionParser transactionParser;

    public ComplexPaymentParser(ITransactionParser transactionParser) {
        this.transactionParser = transactionParser;
    }

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions().size() < 2) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_ILLEGAL_USAGE);
        }

        if (transaction.getData() != null && transaction.getData().size() > 0) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        Transaction txFirst = null;
        List<Transaction> list = new LinkedList<>();

        for (Map.Entry<String, Transaction> entry : transaction.getNestedTransactions().entrySet()) {
            Transaction nestedTx = entry.getValue();

            if (nestedTx.getReference() == null) {

                if (txFirst != null) {
                    throw new ValidateException(Resources.NESTED_TRANSACTION_ILLEGAL_SEQUENCE);
                }

                txFirst = nestedTx;

                AccountID id = transactionParser.getRecipient(txFirst);
                if (id == null) {
                    // invalid configuration (otherwise the parser should throw an exception)
                    throw new UnsupportedOperationException();
                }
                if (!transaction.getSenderID().equals(id)) {
                    throw new ValidateException(Resources.NESTED_TRANSACTION_INVALID_LC);
                }
            } else {
                list.add(nestedTx);
            }
        }

        if (txFirst == null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_SEQUENCE_NOT_FOUND);
        }

        List<ILedgerAction> actions = new LinkedList<>();
        actions.addAll(Arrays.asList(transactionParser.parse(txFirst)));
        for (Transaction txNext : list) {

            if (!txNext.getSenderID().equals(transaction.getSenderID())) {
                throw new ValidateException(Resources.NESTED_TRANSACTION_UNACCEPTABLE_PARAMS);
            }

            AccountID id = transactionParser.getRecipient(txNext);
            if (id == null) {
                // invalid configuration (otherwise the parser should throw an exception)
                throw new UnsupportedOperationException();
            }
            if (!id.equals(txFirst.getSenderID())) {
                throw new ValidateException(Resources.NESTED_TRANSACTION_UNACCEPTABLE_PARAMS);
            }

            if (!txNext.getReference().equals(txFirst.getID())) {
                throw new ValidateException(Resources.NESTED_TRANSACTION_UNACCEPTABLE_PARAMS);
            }

            actions.addAll(Arrays.asList(transactionParser.parse(txNext)));
        }

        actions.add(new FeePaymentAction(transaction.getSenderID(), transaction.getFee()));
        return actions.toArray(new ILedgerAction[0]);
    }

    @Override
    public AccountID getRecipient(Transaction transaction) throws ValidateException {

        Transaction txFirst = null;
        for (Map.Entry<String, Transaction> entry : transaction.getNestedTransactions().entrySet()) {
            Transaction tx = entry.getValue();
            if (tx.getReference() == null) {
                txFirst = tx;
            }
        }
        if (txFirst == null) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_SEQUENCE_NOT_FOUND);
        }
        return txFirst.getSenderID();
    }
}
