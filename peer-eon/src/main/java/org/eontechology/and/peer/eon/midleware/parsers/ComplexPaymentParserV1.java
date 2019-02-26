package org.eontechology.and.peer.eon.midleware.parsers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.middleware.ILedgerAction;
import org.eontechology.and.peer.core.middleware.ITransactionParser;
import org.eontechology.and.peer.eon.midleware.CompositeTransactionParser;
import org.eontechology.and.peer.eon.midleware.Resources;
import org.eontechology.and.peer.eon.midleware.actions.FeePaymentAction;
import org.eontechology.and.peer.tx.TransactionType;

public class ComplexPaymentParserV1 implements ITransactionParser {
    private final ITransactionParser transactionParser;

    public ComplexPaymentParserV1() {
        this(CompositeTransactionParser.create()
                                       .addParser(TransactionType.Payment, new PaymentParser())
                                       .addParser(TransactionType.ColoredCoinPayment, new ColoredCoinPaymentParser())
                                       .build());
    }

    public ComplexPaymentParserV1(ITransactionParser transactionParser) {
        this.transactionParser = transactionParser;
    }

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (transaction.getNestedTransactions().size() < 2) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_ILLEGAL_USAGE);
        }

        if (!transaction.getData().isEmpty()) {
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

                Collection<AccountID> ids = transactionParser.getDependencies(txFirst);
                if (ids == null) {
                    // invalid configuration (otherwise the parser should throw an exception)
                    throw new UnsupportedOperationException();
                }
                if (!ids.contains(transaction.getSenderID())) {
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

            Collection<AccountID> ids = transactionParser.getDependencies(txNext);
            if (ids == null) {
                // invalid configuration (otherwise the parser should throw an exception)
                throw new UnsupportedOperationException();
            }
            if (!ids.contains(txFirst.getSenderID())) {
                throw new ValidateException(Resources.NESTED_TRANSACTION_UNACCEPTABLE_PARAMS);
            }

            if (!txNext.getReference().equals(txFirst.getID())) {
                throw new ValidateException(Resources.NESTED_TRANSACTION_UNACCEPTABLE_PARAMS);
            }

            actions.addAll(Arrays.asList(transactionParser.parse(txNext)));
        }

        actions.add(new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()));
        return actions.toArray(new ILedgerAction[0]);
    }

    @Override
    public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {

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
        return Collections.singleton(txFirst.getSenderID());
    }
}
