package com.exscudo.eon.app.cfg;

import java.util.Objects;

import com.exscudo.eon.app.cfg.forks.ForkItem;
import com.exscudo.eon.app.cfg.forks.Item;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.tx.TransactionType;

/**
 * Initializing the current fork
 */
public class ForkInitializer {

    public static Fork init(BlockID networkID, ForkProperties props) {

        long minDepositSize = props.getMinDepositSize();

        ForkProperties.Period[] periodSet = props.getPeriods();
        Item[] itemSet = new Item[periodSet.length];

        for (int i = 0; i < periodSet.length; i++) {
            ForkProperties.Period period = periodSet[i];
            ForkItem item = new ForkItem(period.getNumber(), period.getDateBegin());

            for (int k = 0; k <= i; k++) {
                ForkProperties.Period p = periodSet[k];

                if (p.getAddedTxTypes() != null) {
                    for (String t : p.getAddedTxTypes()) {
                        Integer type = TransactionType.getType(t);
                        Objects.requireNonNull(type, "Unknown type: " + t);
                        item.addTxType(type);
                    }
                }
                if (p.getRemovedTxTypes() != null) {
                    for (String t : p.getRemovedTxTypes()) {
                        Integer type = TransactionType.getType(t);
                        Objects.requireNonNull(type, "Unknown type: " + t);
                        item.removeTxType(type);
                    }
                }
            }

            itemSet[i] = item;
        }

        Fork fork = new Fork(networkID, itemSet, props.getDateEndAll());

        fork.setMinDepositSize(minDepositSize);

        return fork;
    }
}
