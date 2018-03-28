package com.exscudo.peer.eon.tx;

import com.exscudo.peer.core.common.exceptions.LifecycleException;
import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.data.transaction.ITransactionHandler;
import com.exscudo.peer.core.data.transaction.IValidationRule;
import com.exscudo.peer.core.data.transaction.TransactionContext;
import com.exscudo.peer.core.data.transaction.ValidationResult;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.eon.ledger.ILedgerAction;

public class TransactionHandler implements ITransactionHandler {
    private final ITransactionParser transactionParser;
    private final IValidationRule[] rules;

    public TransactionHandler(ITransactionParser transactionParser, IValidationRule[] rule) {
        this.transactionParser = transactionParser;
        this.rules = rule;
    }

    @Override
    public ILedger run(Transaction transaction, ILedger ledger, TransactionContext context) throws ValidateException {

        if (transaction.isExpired(context.getTimestamp())) {
            throw new LifecycleException();
        }

        for (IValidationRule rule : rules) {
            ValidationResult r = rule.validate(transaction, ledger, context);
            if (r.hasError) {
                throw r.cause;
            }
        }

        ILedgerAction[] actions = transactionParser.parse(transaction);

        ILedger newLedger = ledger;
        for (ILedgerAction action : actions) {
            newLedger = action.run(newLedger, context);
        }

        return newLedger;
    }

    @Override
    public AccountID getRecipient(Transaction transaction) {
        return transactionParser.getRecipient(transaction);
    }
}
