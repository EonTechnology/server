package org.eontechnology.and.eon.app.cfg;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Set;

import org.eontechnology.and.eon.app.cfg.forks.ForkItem;
import org.eontechnology.and.eon.app.cfg.forks.Item;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.IFork;
import org.eontechnology.and.peer.core.data.Account;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.ledger.ILedger;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;

/**
 * Basic implementation of the {@code IFork} interface.
 */
public class Fork implements IFork {
    private final LinkedList<ForkItem> items;
    private final BlockID genesisBlockID;
    private long minDepositSize = Long.MAX_VALUE;

    public Fork(BlockID genesisBlockID, ForkItem[] items, String beginAll) {
        this(genesisBlockID, items, Instant.parse(beginAll).toEpochMilli());
    }

    public Fork(BlockID genesisBlockID, ForkItem[] items) {
        this(genesisBlockID, items, 0L);
    }

    public Fork(BlockID genesisBlockID, ForkItem[] items, long beginAll) {
        this.genesisBlockID = genesisBlockID;

        this.items = new LinkedList<>();
        Collections.addAll(this.items, items);

        // Sort by number desc
        this.items.sort(new ItemComparator());

        long begin = beginAll;
        for (int i = this.items.size() - 1; i >= 0; i--) {
            Item item = this.items.get(i);
            item.setBegin(begin);
            begin = item.getEnd();
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
