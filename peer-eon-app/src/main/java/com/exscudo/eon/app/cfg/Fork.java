package com.exscudo.eon.app.cfg;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Set;

import com.exscudo.eon.app.cfg.forks.ForkItem;
import com.exscudo.eon.app.cfg.forks.Item;
import com.exscudo.peer.core.Constant;
import com.exscudo.peer.core.IFork;
import com.exscudo.peer.core.data.Account;
import com.exscudo.peer.core.data.identifier.BlockID;
import com.exscudo.peer.core.ledger.ILedger;
import com.exscudo.peer.core.middleware.ITransactionParser;

/**
 * Basic implementation of the {@code IFork} interface.
 */
public class Fork implements IFork {
    private final LinkedList<ForkItem> items;
    private final BlockID genesisBlockID;
    private long minDepositSize = Long.MAX_VALUE;

    public Fork(BlockID genesisBlockID, ForkItem[] items, String endAll) {
        this.genesisBlockID = genesisBlockID;

        this.items = new LinkedList<>();
        Collections.addAll(this.items, items);

        // Sort by number desc
        this.items.sort(new ItemComparator());

        long end = Instant.parse(endAll).toEpochMilli();
        for (int i = 0; i < this.items.size(); i++) {
            Item item = this.items.get(i);
            item.setEnd(end);
            end = item.getBegin();
        }
    }

    @Override
    public BlockID getGenesisBlockID() {
        return genesisBlockID;
    }

    @Override
    public boolean isPassed(int timestamp) {
        return items.getFirst().isPassed(timestamp);
    }

    @Override
    public boolean isCome(int timestamp) {
        return items.getFirst().isCome(timestamp);
    }

    @Override
    public int getNumber(int timestamp) {
        Item item = getItem(timestamp);
        return (item == null) ? -1 : item.getNumber();
    }

    @Override
    public Set<Integer> getTransactionTypes(int timestamp) {
        Item item = getItem(timestamp);
        return (item == null) ? null : item.getTransactionTypes();
    }

    @Override
    public int getBlockVersion(int timestamp) {
        Item item = getItem(timestamp);
        return (item == null) ? -1 : item.getBlockVersion();
    }

    @Override
    public ILedger convert(ILedger ledger, int timestamp) {
        Item item = getItem(timestamp);

        if (item == null) {
            return ledger;
        }

        if (!item.needConvertAccounts()) {
            return ledger;
        }

        if (getNumber(timestamp - Constant.BLOCK_PERIOD) == item.getNumber()) {
            return ledger;
        }

        ILedger newLedger = ledger;
        for (Account account : ledger) {
            newLedger = newLedger.putAccount(item.convert(account));
        }
        return newLedger;
    }

    @Override
    public ITransactionParser getParser(int timestamp) {
        Item item = getItem(timestamp);
        return (item == null) ? null : item.getParser();
    }

    ForkItem getItem(int timestamp) {

        if (items.getFirst().isCome(timestamp)) {
            return items.getFirst();
        }

        if (!items.getLast().isCome(timestamp)) {
            return null;
        }

        for (ForkItem item : items) {
            if (item.isCome(timestamp) && !item.isPassed(timestamp)) {
                return item;
            }
        }

        throw new UnsupportedOperationException();
    }

    public long getMinDepositSize() {
        return minDepositSize;
    }

    public void setMinDepositSize(long minDepositSize) {
        this.minDepositSize = minDepositSize;
    }

    private static class ItemComparator implements Comparator<Item> {
        @Override
        public int compare(Item o1, Item o2) {
            // Desc
            return Integer.compare(o2.getNumber(), o1.getNumber());
        }
    }
}
