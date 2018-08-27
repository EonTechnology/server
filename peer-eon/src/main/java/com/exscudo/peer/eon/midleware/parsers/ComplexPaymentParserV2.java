package com.exscudo.peer.eon.midleware.parsers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.identifier.TransactionID;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.eon.midleware.CompositeTransactionParser;
import com.exscudo.peer.eon.midleware.Resources;
import com.exscudo.peer.eon.midleware.actions.FeePaymentAction;
import com.exscudo.peer.tx.TransactionType;

public class ComplexPaymentParserV2 implements ITransactionParser {
    private final ITransactionParser transactionParser;

    public ComplexPaymentParserV2() {
        this(CompositeTransactionParser.create()
                                       .addParser(TransactionType.Payment, new PaymentParser())
                                       .addParser(TransactionType.ColoredCoinPayment, new ColoredCoinPaymentParser())
                                       .build());
    }

    public ComplexPaymentParserV2(ITransactionParser transactionParser) {
        this.transactionParser = transactionParser;
    }

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (!transaction.getData().isEmpty()) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        HashMap<TransactionID, Transaction> txFirst = new HashMap<>();
        HashMap<TransactionID, Transaction> txRef = new HashMap<>();

        for (Transaction nTX : transaction.getNestedTransactions().values()) {
            if (nTX.getReference() == null) {
                txFirst.put(nTX.getID(), nTX);
            } else {
                txRef.put(nTX.getID(), nTX);
            }
        }

        List<ILedgerAction> actions = new LinkedList<>();

        for (Transaction nTX : txFirst.values()) {
            if (!transaction.getSenderID().equals(nTX.getPayer())) {
                throw new ValidateException(Resources.NESTED_TRANSACTION_PAYER_ERROR);
            }
            actions.addAll(Arrays.asList(transactionParser.parse(nTX)));
        }

        for (Transaction nTX : txRef.values()) {
            if (nTX.getPayer() != null) {
                throw new ValidateException(Resources.NESTED_TRANSACTION_PAYER_SEQUENCE_ERROR);
            }
            if (!txFirst.containsKey(nTX.getReference())) {
                throw new ValidateException(Resources.NESTED_TRANSACTION_SEQUENCE_NOT_FOUND);
            }
            actions.addAll(Arrays.asList(transactionParser.parse(nTX)));
        }

        actions.add(new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()));
        return actions.toArray(new ILedgerAction[0]);
    }

    @Override
    public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {

        HashSet<AccountID> accSet = new HashSet<>();
        for (Transaction tx : transaction.getNestedTransactions().values()) {
            accSet.add(tx.getSenderID());
            Collection<AccountID> recipients = transactionParser.getDependencies(tx);
            if (recipients != null) {
                accSet.addAll(recipients);
            }
        }

        return accSet;
    }
}
