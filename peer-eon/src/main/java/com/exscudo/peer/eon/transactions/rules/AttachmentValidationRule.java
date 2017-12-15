package com.exscudo.peer.eon.transactions.rules;

import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.services.ILedger;
import com.exscudo.peer.core.services.TransactionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AttachmentValidationRule implements IValidationRule {

    private final Map<Integer, IValidationRule> dictionary = new HashMap<>();

    public AttachmentValidationRule() {
    }

    public AttachmentValidationRule(Map<Integer, IValidationRule> rules) {
        for (Map.Entry<Integer, IValidationRule> entry : rules.entrySet()) {
            this.bind(entry.getKey(), entry.getValue());
        }
    }

    public synchronized void bind(int txType, IValidationRule handler) {
        if (dictionary.containsKey(txType)) {
            throw new IllegalArgumentException("The value is already in the dictionary.");
        }
        dictionary.put(txType, handler);
    }

    private synchronized IValidationRule getItem(int type) {
        return dictionary.get(type);
    }

    @Override
    public ValidationResult validate(Transaction transaction, ILedger ledger, TransactionContext context) {
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(context);

        IValidationRule rule = this.getItem(transaction.getType());
        if (rule == null) {
            return ValidationResult.error("Invalid transaction type. Type :" + transaction.getType());
        }
        return rule.validate(transaction, ledger, context);
    }
}
