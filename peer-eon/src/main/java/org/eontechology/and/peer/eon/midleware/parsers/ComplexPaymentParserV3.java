package org.eontechology.and.peer.eon.midleware.parsers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.eontechology.and.peer.core.common.exceptions.ValidateException;
import org.eontechology.and.peer.core.data.Transaction;
import org.eontechology.and.peer.core.data.identifier.AccountID;
import org.eontechology.and.peer.core.data.identifier.TransactionID;
import org.eontechology.and.peer.core.middleware.ILedgerAction;
import org.eontechology.and.peer.core.middleware.ITransactionParser;
import org.eontechology.and.peer.eon.midleware.CompositeTransactionParser;
import org.eontechology.and.peer.eon.midleware.Resources;
import org.eontechology.and.peer.eon.midleware.actions.FeePaymentAction;
import org.eontechology.and.peer.tx.TransactionType;

public class ComplexPaymentParserV3 implements ITransactionParser {
    private final ITransactionParser transactionParser;

    public ComplexPaymentParserV3() {
        this(CompositeTransactionParser.create()
                                       .addParser(TransactionType.Payment, new PaymentParser())
                                       .addParser(TransactionType.ColoredCoinPayment, new ColoredCoinPaymentParser())
                                       .build());
    }

    public ComplexPaymentParserV3(ITransactionParser transactionParser) {
        this.transactionParser = transactionParser;
    }

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {

        if (!transaction.getData().isEmpty()) {
            throw new ValidateException(Resources.ATTACHMENT_UNKNOWN_TYPE);
        }

        List<Transaction> txFirst = new LinkedList<>();
        HashMap<TransactionID, List<Transaction>> txRef = new HashMap<>();

        for (Transaction nTX : transaction.getNestedTransactions().values()) {
            if (nTX.getReference() == null) {

                if (!transaction.getSenderID().equals(nTX.getPayer())) {
                    throw new ValidateException(Resources.NESTED_TRANSACTION_PAYER_ERROR);
                }

                txFirst.add(nTX);
            } else {

                if (nTX.getPayer() != null) {
                    throw new ValidateException(Resources.NESTED_TRANSACTION_PAYER_SEQUENCE_ERROR);
                }

                List<Transaction> list = txRef.get(nTX.getReference());
                if (list == null) {
                    list = new LinkedList<>();
                    txRef.put(nTX.getReference(), list);
                }
                list.add(nTX);
            }
        }

        List<ILedgerAction> actions = getActions(txFirst, txRef);
        if (txRef.size() != 0) {
            throw new ValidateException(Resources.NESTED_TRANSACTION_SEQUENCE_NOT_FOUND);
        }

        actions.add(new FeePaymentAction(transaction.getSenderID(), transaction.getPayer(), transaction.getFee()));
        return actions.toArray(new ILedgerAction[0]);
    }

    private List<ILedgerAction> getActions(List<Transaction> txSet,
                                           HashMap<TransactionID, List<Transaction>> txRef) throws ValidateException {

        List<ILedgerAction> actions = new LinkedList<>();

        for (Transaction nTX : txSet) {
            actions.addAll(Arrays.asList(transactionParser.parse(nTX)));

            if (txRef.containsKey(nTX.getID())) {
                actions.addAll(getActions(txRef.get(nTX.getID()), txRef));
                txRef.remove(nTX.getID());
            }
        }

        return actions;
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
