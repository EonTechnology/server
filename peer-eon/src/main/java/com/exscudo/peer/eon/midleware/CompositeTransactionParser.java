package com.exscudo.peer.eon.midleware;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.NamingException;

import com.exscudo.peer.core.common.exceptions.ValidateException;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.data.identifier.AccountID;
import com.exscudo.peer.core.middleware.ILedgerAction;
import com.exscudo.peer.core.middleware.ITransactionParser;

public class CompositeTransactionParser implements ITransactionParser {
    private final Map<Integer, ITransactionParser> dictionary;

    public CompositeTransactionParser(Map<Integer, ITransactionParser> map) {
        dictionary = new ConcurrentHashMap<>(map);
    }

    public static Builder create() {
        return new Builder();
    }

    private ITransactionParser getParser(int type) throws NamingException {
        ITransactionParser parser = dictionary.get(type);
        if (parser == null) {
            throw new NamingException();
        }
        return parser;
    }

    @Override
    public ILedgerAction[] parse(Transaction transaction) throws ValidateException {
        Objects.requireNonNull(transaction);

        ITransactionParser parser;
        try {
            parser = getParser(transaction.getType());
        } catch (NamingException e) {
            throw new ValidateException(Resources.TRANSACTION_TYPE_UNKNOWN);
        }
        return parser.parse(transaction);
    }

    @Override
    public Collection<AccountID> getDependencies(Transaction transaction) throws ValidateException {

        ITransactionParser parser;
        try {
            parser = getParser(transaction.getType());
        } catch (NamingException e) {
            throw new ValidateException(Resources.TRANSACTION_TYPE_UNKNOWN);
        }
        return parser.getDependencies(transaction);
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
