package com.exscudo.peer.eon.tx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.eon.ledger.ILedgerAction;

public class CompositeTransactionParser implements ITransactionParser {
    private final Map<Integer, ITransactionParser> dictionary;

    private CompositeTransactionParser(Map<Integer, ITransactionParser> map) {
        dictionary = new ConcurrentHashMap<>(map);
    }

    public static Builder create() {
        return new Builder();
    }

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {
        Objects.requireNonNull(transaction);

        ITransactionParser rule = dictionary.get(transaction.getType());
        if (rule == null) {
            throw new ValidateException("Invalid transaction type. Type :" + transaction.getType());
        }
        return rule.parse(transaction);
    }

    public static class Builder {
        private final Map<Integer, ITransactionParser> dictionary = new HashMap<>();

        public Builder addParser(int txType, ITransactionParser handler) {
            if (dictionary.containsKey(txType)) {
                throw new IllegalArgumentException("The value is already in the dictionary.");
            }
            dictionary.put(txType, handler);
            return this;
        }

        public CompositeTransactionParser build() {
            return new CompositeTransactionParser(dictionary);
        }
    }
}
