package com.exscudo.eon.app.cfg;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.exscudo.eon.app.cfg.forks.ForkItem;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.middleware.ITransactionParser;
import com.exscudo.peer.tx.TransactionType;

/**
 * Initializing the current fork
 */
public class ForkInitializer {

    public static Fork init(BlockID networkID, ForkProperties props) throws IOException {

        long minDepositSize = props.getMinDepositSize();

        ForkProperties.Period[] periodSet = props.getPeriods();
        ForkItem[] itemSet = new ForkItem[periodSet.length];

        for (int i = 0; i < periodSet.length; i++) {
            ForkProperties.Period period = periodSet[i];
            ForkItem item = new ForkItem(period.getNumber(), period.getDateBegin());

            for (int k = 0; k <= i; k++) {
                ForkProperties.Period p = periodSet[k];

                if (p.getRemovedTxTypes() != null) {
                    for (String t : p.getRemovedTxTypes()) {
                        Integer type = TransactionType.getType(t);
                        Objects.requireNonNull(type, "Unknown type: " + t);

                        item.removeTxType(type);
                    }
                }

                if (p.getAddedTxTypes() != null) {
                    for (Map.Entry<String, String> entry : p.getAddedTxTypes().entrySet()) {
                        Integer type = TransactionType.getType(entry.getKey());
                        Objects.requireNonNull(type, "Unknown type: " + entry.getKey());

                        try {

                            Class<?> clazz = Class.forName(entry.getValue());
                            if (!ITransactionParser.class.isAssignableFrom(clazz)) {
                                throw new ClassCastException();
                            }
                            Constructor<?> ctor = clazz.getConstructor();
                            Object obj = ctor.newInstance();
                            item.addTxType(type, (ITransactionParser) obj);
                        } catch (Exception e) {
                            throw new IOException(e);
                        }
                    }
                }

                List<String> rules = item.getValidationRules();
                if (rules == null) {
                    rules = new LinkedList<>();
                }

                if (p.getRemovedRules() != null) {
                    for (String r : p.getRemovedRules()) {
                        if (!rules.remove(r)) {
                            throw new IllegalArgumentException();
                        }
                    }
                }

                if (p.getAddedRules() != null) {
                    for (String r : p.getAddedRules()) {
                        if (rules.contains(r)) {
                            throw new IllegalArgumentException();
                        }
                        rules.add(r);
                    }
                }
                item.setValidationRules(rules);
            }

            itemSet[i] = item;
        }

        Fork fork = new Fork(networkID, itemSet, props.getDateEndAll());

        fork.setMinDepositSize(minDepositSize);

        return fork;
    }
}
